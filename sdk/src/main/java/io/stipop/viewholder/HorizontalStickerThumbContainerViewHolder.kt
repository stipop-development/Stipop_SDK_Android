package io.stipop.viewholder

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.stipop.Config
import io.stipop.R
import io.stipop.Utils
import io.stipop.adapter.HorizontalPackageAdapter
import io.stipop.custom.RecyclerDecoration
import io.stipop.databinding.ItemHorizontalStickerThumbContainerBinding
import io.stipop.models.StickerPackage
import io.stipop.setStipopUnderlineColor
import io.stipop.viewholder.delegates.StickerPackageClickDelegate

internal class HorizontalStickerThumbContainerViewHolder(
    private val binding: ItemHorizontalStickerThumbContainerBinding,
    val delegate: StickerPackageClickDelegate?
) :
    RecyclerView.ViewHolder(binding.root) {

    private val adapter: HorizontalPackageAdapter by lazy { HorizontalPackageAdapter(delegate = delegate) }
    private val decoration = RecyclerDecoration(Utils.dpToPx(7F).toInt())

    init {
        with(binding) {
            underLine.setStipopUnderlineColor()
            titleTextView.setTextColor(Config.getTitleTextColor(itemView.context))
            recyclerView.removeItemDecoration(decoration)
            recyclerView.addItemDecoration(decoration)
            recyclerView.addOnItemTouchListener(listener)
        }
    }

    fun bind(title: String, stickerPackages: List<StickerPackage>) {
        with(binding) {
            titleTextView.text = title
            recyclerView.adapter = adapter
            adapter.clearData()
            adapter.updateData(stickerPackages)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            delegate: StickerPackageClickDelegate?
        ): HorizontalStickerThumbContainerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_horizontal_sticker_thumb_container, parent, false)
            val binding = ItemHorizontalStickerThumbContainerBinding.bind(view)
            return HorizontalStickerThumbContainerViewHolder(binding, delegate)
        }

        val listener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                rv.parent.requestDisallowInterceptTouchEvent(true)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                //
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                //
            }
        }
    }
}