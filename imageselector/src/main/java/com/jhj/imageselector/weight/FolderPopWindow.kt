package com.jhj.imageselector.weight

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.jhj.imageselector.bean.LocalMediaFolder
import com.jhj.imageselector.R
import com.jhj.slimadapter.SlimAdapter
import kotlinx.android.synthetic.main.layout_image_selector_folder.view.*

/**
 * 自定义PopWindow
 * Created by jhj on 19-1-19.
 */
class FolderPopWindow(private val mContext: Context) : PopupWindow() {

    private var isDismiss = false
    private var view: View
    private var adapter: SlimAdapter

    open val mIsAnim = true
    open val mShowAnim = R.anim.anim_folder_in
    open val mDismissAnim = R.anim.anim_folder_out
    open val folderSize = 6


    init {
        val localDisplayMetrics = DisplayMetrics()
        (mContext as Activity).windowManager.defaultDisplay.getMetrics(localDisplayMetrics)

        view = LayoutInflater.from(mContext).inflate(R.layout.layout_image_selector_folder, null)
        contentView = view
        width = localDisplayMetrics.widthPixels
        height = localDisplayMetrics.heightPixels
        isFocusable = true
        isOutsideTouchable = true
        update()
        setBackgroundDrawable(ColorDrawable(Color.argb(123, 0, 0, 0)))

        view.layout_image_selector_folder.setOnClickListener {
            dismiss()
        }
        val density = mContext.resources.displayMetrics.density
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (folderSize * 60 * density).toInt())
        view.recycler_image_selector_folder.layoutParams = params
        adapter = SlimAdapter.creator()
                .register<LocalMediaFolder>(R.layout.layout_image_selector_folder_item) { injector, bean, position ->
                    injector
                            .with<ImageView>(R.id.iv_folder_image) {
                                val options = RequestOptions()
                                        .placeholder(R.mipmap.ic_image_selector_placeholder)
                                        .sizeMultiplier(0.5f)
                                        .transform(RoundedCorners(8))
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                Glide.with(mContext)
                                        .asBitmap()
                                        .load(bean.firstImagePath)
                                        .apply(options)
                                        .into(it)
                            }
                            .text(R.id.tv_folder_name, bean.name)
                            .text(R.id.tv_folder_num, "(${bean.imageNum})")
                            .clicked {
                                itemClickedListener?.onItemClicked(bean)
                                dismiss()
                            }
                }
                .attachTo(view.recycler_image_selector_folder)


    }

    var itemClickedListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClicked(bean: LocalMediaFolder)
    }

    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickedListener = onItemClickListener
    }


    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int) {
        super.showAsDropDown(anchor, xoff, yoff)
    }

    override fun showAsDropDown(anchor: View) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                val rect = Rect()
                anchor.getGlobalVisibleRect(rect)
                val h = anchor.resources.displayMetrics.heightPixels - rect.bottom
                height = h
            }
            super.showAsDropDown(anchor)
            isDismiss = false
            if (mIsAnim) {
                val animationIn = AnimationUtils.loadAnimation(mContext, mShowAnim)
                view.startAnimation(animationIn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dismiss() {
        if (mIsAnim) {
            if (isDismiss) {
                return
            }
            isDismiss = true
            val animationOut = AnimationUtils.loadAnimation(mContext, mDismissAnim)
            view.startAnimation(animationOut)
            dismiss()
            animationOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    isDismiss = false
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                        Handler().post { super@FolderPopWindow.dismiss() }
                    } else {
                        super@FolderPopWindow.dismiss()
                    }
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        } else {
            super@FolderPopWindow.dismiss()
        }
    }


    fun bindFolder(it: List<LocalMediaFolder>) {
        adapter.setDataList(it)
    }
}