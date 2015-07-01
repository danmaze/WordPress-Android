package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPWebViewClient;

public class PostPreviewActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_POST_ID = "local_post_id";
    public static final String ARG_IS_PAGE = "is_page";

    private WebView mWebView;
    private TextView mTitleTextView;
    private TextView mContentTextView;
    private long mLocalPostId;
    private boolean mIsPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_preview_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        mTitleTextView = (TextView) findViewById(R.id.postTitle);
        mContentTextView = (TextView) findViewById(R.id.viewPostTextView);

        mWebView = (WebView) findViewById(R.id.viewPostWebView);
        mWebView.setWebViewClient(new WPWebViewClient(WordPress.getCurrentBlog()));

        if (savedInstanceState != null) {
            mLocalPostId = savedInstanceState.getLong(ARG_LOCAL_POST_ID);
            mIsPage = savedInstanceState.getBoolean(ARG_IS_PAGE);
        } else {
            mLocalPostId = getIntent().getLongExtra(ARG_LOCAL_POST_ID, 0);
            mIsPage = getIntent().getBooleanExtra(ARG_IS_PAGE, false);
        }

        loadPost();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.post_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            ActivityLauncher.editBlogPostOrPageForResult(this, mLocalPostId, mIsPage);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.EDIT_POST) {
            loadPost();
        }
    }

    private void loadPost() {
        if (isFinishing()) {
            return;
        }

        final Post post = WordPress.wpDB.getPostForLocalTablePostId(mLocalPostId);
        if (post == null) {
            ToastUtils.showToast(this, R.string.post_not_found);
            finish();
            return;
        }

        final Handler handler = new Handler();

        // locate views and determine content in the background to avoid ANR - especially
        // important when using WPHtml.fromHtml() for drafts that contain images since
        // thumbnails may take some time to create
        new Thread() {
            @Override
            public void run() {
                final String title = (TextUtils.isEmpty(post.getTitle())
                        ? "(" + getResources().getText(R.string.untitled) + ")"
                        : StringUtils.unescapeHTML(post.getTitle()));

                final String postContent = post.getDescription() + "\n\n" + post.getMoreText();

                final Spanned draftContent;
                final String htmlContent;
                if (post.isLocalDraft()) {
                    Point point = DisplayUtils.getDisplayPixelSize(PostPreviewActivity.this);
                    int maxWidth = Math.min(point.x, point.y);
                    draftContent = WPHtml.fromHtml(postContent.replaceAll("\uFFFC", ""), PostPreviewActivity.this, post, maxWidth);
                    htmlContent = null;
                } else {
                    draftContent = null;
                    htmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                            + "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"webview.css\" /></head>"
                            + "<body><div id=\"container\">"
                            + StringUtils.addPTags(postContent)
                            + "</div></body></html>";
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;

                        mTitleTextView.setText(title);

                        if (post.isLocalDraft()) {
                            mContentTextView.setVisibility(View.VISIBLE);
                            mWebView.setVisibility(View.GONE);
                            mContentTextView.setText(draftContent);
                        } else {
                            mContentTextView.setVisibility(View.GONE);
                            mWebView.setVisibility(View.VISIBLE);
                            mWebView.loadDataWithBaseURL(
                                    "file:///android_asset/",
                                    htmlContent,
                                    "text/html",
                                    "utf-8",
                                    null);
                        }
                    }
                });
            }
        }.start();
    }
}
