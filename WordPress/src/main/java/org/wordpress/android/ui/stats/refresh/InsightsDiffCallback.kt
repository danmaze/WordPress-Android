package org.wordpress.android.ui.stats.refresh

import android.support.v7.util.DiffUtil.Callback

class InsightsDiffCallback(
    private val oldList: List<InsightsItem>,
    private val newList: List<InsightsItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].isTheSame(newList[newItemPosition])
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
