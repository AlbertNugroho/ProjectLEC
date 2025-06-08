package com.example.lecproject

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

class NonscrollableGridLayout(context: Context, spanCount: Int) : GridLayoutManager(context, spanCount) {
    override fun canScrollVertically(): Boolean = false
}
