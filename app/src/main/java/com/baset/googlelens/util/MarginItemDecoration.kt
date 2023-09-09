package com.baset.googlelens.util

import android.graphics.Rect
import android.view.View
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView

class MarginItemDecoration(
    @IntRange(from = 0) val marginTop: Int,
    @IntRange(from = 0) val leftMargin: Int,
    @IntRange(from = 0) val rightMargin: Int,
    @IntRange(from = 0) val bottomMargin: Int
) :
    RecyclerView.ItemDecoration() {

    constructor(@IntRange(from = 0) margin: Int) : this(margin, margin, margin, margin)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            if (parent.getChildAdapterPosition(view) == 0) {
                top = marginTop
            }
            left = leftMargin
            right = rightMargin
            bottom = bottomMargin
        }
    }
}