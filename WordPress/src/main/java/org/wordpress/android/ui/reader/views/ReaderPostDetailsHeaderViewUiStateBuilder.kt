package org.wordpress.android.ui.reader.views

import dagger.Reusable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostTagsUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderViewUiState.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import javax.inject.Inject

@Reusable
class ReaderPostDetailsHeaderViewUiStateBuilder @Inject constructor(
    private val accountStore: AccountStore,
    private val postUiStateBuilder: ReaderPostUiStateBuilder,
    private val readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder
) {
    fun mapPostToUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long?, Long?) -> Unit,
        onFollowClicked: () -> Unit,
        onTagItemClicked: (String) -> Unit
    ): ReaderPostDetailsHeaderUiState {
        val hasAccessToken = accountStore.hasAccessToken()
        val textTitle = post
                .takeIf { post.hasTitle() }
                ?.title?.let { UiStringText(it) } ?: UiStringRes(string.reader_untitled_post)

        return ReaderPostDetailsHeaderUiState(
                title = textTitle,
                authorName = post.authorName,
                tagItems = buildTagItems(post, onTagItemClicked),
                tagItemsVisibility = buildTagItemsVisibility(post),
                blogSectionUiState = buildBlogSectionUiState(post, onBlogSectionClicked),
                followButtonUiState = buildFollowButtonUiState(onFollowClicked, post, hasAccessToken)
        )
    }

    private fun buildBlogSectionUiState(
        post: ReaderPost,
        onBlogSectionClicked: (Long?, Long?) -> Unit
    ): ReaderBlogSectionUiState {
        return postUiStateBuilder.mapPostToBlogSectionUiState(
            post,
            onBlogSectionClicked
        )
    }

    private fun buildFollowButtonUiState(
        onFollowClicked: () -> Unit,
        post: ReaderPost,
        hasAccessToken: Boolean
    ): FollowButtonUiState {
        return FollowButtonUiState(
            onFollowButtonClicked = onFollowClicked,
            isFollowed = post.isFollowedByCurrentUser,
            isEnabled = hasAccessToken,
            isVisible = hasAccessToken
        )
    }

    private fun buildTagItems(post: ReaderPost, onClicked: (String) -> Unit) =
            readerPostTagsUiStateBuilder.mapPostTagsToTagUiStates(post, onClicked)

    private fun buildTagItemsVisibility(post: ReaderPost) = post.tags.isNotEmpty()
}