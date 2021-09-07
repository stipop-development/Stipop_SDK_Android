package io.stipop.refactor.present.ui.listeners

import androidx.recyclerview.widget.RecyclerView

interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder?)

}
