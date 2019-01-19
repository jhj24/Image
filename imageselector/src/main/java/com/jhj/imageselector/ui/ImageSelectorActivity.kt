package com.jhj.imageselector.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.text.TextUtils
import android.view.animation.Animation
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.jhj.imageselector.*
import com.jhj.imageselector.weight.PopWindow
import com.jhj.slimadapter.SlimAdapter
import com.jhj.slimadapter.holder.ViewInjector
import kotlinx.android.synthetic.main.activity_image_selector.*
import kotlinx.android.synthetic.main.layout_image_selector_topbar.*
import org.jetbrains.anko.toast
import java.io.File
import java.util.*

/**
 * 图片选择
 *
 * Created by jhj on 19-1-16.
 */
open class ImageSelectorActivity : AppCompatActivity() {

    //动画
    private val zoomAnim: Boolean = false
    private val originalSize = 1.0f
    private val zoomSize = 1.10f
    private val DURATION = 450

    //模式
    private var isOnlyTakePhoto = false //是否只拍照
    private var isAllowTakePhoto = true //选择照片是否相机
    private var mSelectMode = PictureConfig.SINGLE //图片是单选、多选


    //popWindow
    private var isFolderWindowDismiss = true

    //选择框
    private lateinit var folderWindow: PopWindow
    private var foldersList: List<LocalMediaFolder> = ArrayList()


    private lateinit var config: PictureSelectionConfig
    private var cameraPath: String? = null
    private var outputCameraPath: String? = null
    private var selectImages = ArrayList<LocalMedia>()
    private val maxSelectNum: Int = 9

    private lateinit var animation: Animation
    private lateinit var adapter: SlimAdapter
    private lateinit var lastTimeSelectedInjector: ViewInjector
    private lateinit var lastTimeSelectedLocalMedia: LocalMedia

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_selector)
        config = PictureSelectionConfig.getInstance()


        val titleDrawableRight = if (isFolderWindowDismiss) {
            R.drawable.arrow_down
        } else {
            R.drawable.arrow_up
        }
        val drawable = ContextCompat.getDrawable(this, titleDrawableRight)
        tv_image_selector_title.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        tv_image_selector_title.compoundDrawablePadding = 10
        tv_image_selector_title.setOnClickListener {
            folderWindow.showAsDropDown(it)
        }

        if (isOnlyTakePhoto) {
             startOpenCamera()
             return
        }


        folderWindow = PopWindow(this)

        animation = OptAnimationLoader.loadAnimation(this, R.anim.modal_in)

        picture_recycler.setHasFixedSize(true)
        picture_recycler.addItemDecoration(GridSpacingItemDecoration(4,
                (2 * resources.displayMetrics.density).toInt(), false))
        picture_recycler.layoutManager = GridLayoutManager(this, 4)

        MediaLoading.loadMedia(this, false) {
            foldersList = it
            val list = arrayListOf<Any>()
            folderWindow.bindFolder(it)
            if (isAllowTakePhoto) {
                list.add(Camera())
            }
            list.addAll(it[0].images)

            adapter = SlimAdapter.creator(GridLayoutManager(this, 4))
                    .register<LocalMedia>(R.layout.layout_grid_image) { viewInjector, localMedia, i ->
                        viewInjector
                                .with<ImageView>(R.id.iv_image_selector_picture) {
                                    Glide.with(this)
                                            .load(localMedia.path)
                                            .into(it)

                                }
                                .with<ImageView>(R.id.iv_image_selector_state) {
                                    it.isSelected = localMedia.isChecked
                                    if (mSelectMode == PictureConfig.SINGLE && localMedia.isChecked) {
                                        lastTimeSelectedInjector = viewInjector
                                        lastTimeSelectedLocalMedia = localMedia
                                    }
                                }
                                .clicked(R.id.layout_image_selector_state) {


                                }
                                .clicked {
                                    val pictureType = if (selectImages.size > 0) selectImages.get(0).pictureType else ""
                                    val stateImageView = viewInjector.getView<ImageView>(R.id.iv_image_selector_state)
                                    val imageView = viewInjector.getView<ImageView>(R.id.iv_image_selector_picture)
                                    val isChecked = stateImageView.isSelected
                                    if (!TextUtils.isEmpty(pictureType)) {
                                        val toEqual = MediaMimeType.mimeToEqual(pictureType, localMedia.pictureType)
                                        if (!toEqual) {
                                            toast("不能同时选择图片或视频")
                                            return@clicked
                                        }
                                    }
                                    //达到最大选择数，点击未选中的ImageView
                                    if (selectImages.size >= maxSelectNum && !isChecked) {
                                        val eqImg = pictureType.startsWith(PictureConfig.IMAGE)
                                        val str = if (eqImg)
                                            "你最多可以选择${maxSelectNum}张图片"
                                        else
                                            "你最多可以选择${maxSelectNum}个视频"
                                        toast(str)
                                        return@clicked
                                    }


                                    stateImageView.isSelected = !isChecked
                                    localMedia.isChecked = !isChecked

                                    if (stateImageView.isSelected) {
                                        // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                                        if (mSelectMode == PictureConfig.SINGLE) {
                                            singleRadioMediaImage()
                                            lastTimeSelectedInjector = viewInjector
                                            lastTimeSelectedLocalMedia = localMedia
                                        }
                                        selectImages.add(localMedia)
                                        localMedia.num = selectImages.size
                                        scaleAnim(imageView, originalSize, zoomSize)
                                        imageView.setColorFilter(ContextCompat.getColor(this, R.color.image_overlay_true), PorterDuff.Mode.SRC_ATOP)
                                    } else {
                                        selectImages.remove(localMedia)
                                        scaleAnim(imageView, zoomSize, originalSize)
                                        imageView.setColorFilter(ContextCompat.getColor(this, R.color.image_overlay_false), PorterDuff.Mode.SRC_ATOP)
                                    }
                                }
                    }
                    .register<Camera>(R.layout.layout_item_camera) { viewInjector, localMedia, i ->
                        viewInjector.clicked {
                            if (selectImages.size >= maxSelectNum) {
                                toast("你最多可以选择${maxSelectNum}张图片")
                                return@clicked
                            }
                            startOpenCamera()
                        }
                    }
                    .attachTo(picture_recycler)
                    .setDataList(list)
        }
    }


    private fun startOpenCamera() {
        //Todo 权限请求
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            val type = if (config.mimeType == PictureConfig.TYPE_ALL)
                PictureConfig.TYPE_IMAGE
            else
                config.mimeType
            val cameraFile = PictureFileUtils.createCameraFile(this, type, outputCameraPath, config.suffixType)
            cameraPath = cameraFile.absolutePath
            val imageUri = parUri(cameraFile)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(cameraIntent, PictureConfig.REQUEST_CAMERA)
        }
    }

    /**
     * 生成uri
     *
     * @param cameraFile
     * @return
     */
    private fun parUri(cameraFile: File): Uri {
        val imageUri: Uri
        val authority = packageName + ".provider"
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            //通过FileProvider创建一个content类型的Uri
            imageUri = FileProvider.getUriForFile(this, authority, cameraFile)
        } else {
            imageUri = Uri.fromFile(cameraFile)
        }
        return imageUri
    }


    /**
     * 单选模式
     */
    private fun singleRadioMediaImage() {
        if (selectImages.size > 0) {
            selectImages.clear()
            val lastTimeSelectedImageView = lastTimeSelectedInjector.getView<ImageView>(R.id.iv_image_selector_picture)
            val lastTimeSelectedStateImageView = lastTimeSelectedInjector.getView<ImageView>(R.id.iv_image_selector_state)
            scaleAnim(lastTimeSelectedImageView, zoomSize, originalSize)
            lastTimeSelectedImageView.setColorFilter(ContextCompat.getColor(this, R.color.image_overlay_false), PorterDuff.Mode.SRC_ATOP)
            lastTimeSelectedStateImageView.isSelected = false
            lastTimeSelectedLocalMedia.isChecked = false
        }
    }


    /**
     * 用于ImageView选中、取消图片放大、缩小
     */
    private fun scaleAnim(imageView: ImageView, startSize: Float, endSize: Float) {
        val set = AnimatorSet()
        set.playTogether(
                ObjectAnimator.ofFloat(imageView, "scaleX", startSize, endSize),
                ObjectAnimator.ofFloat(imageView, "scaleY", startSize, endSize)
        )
        set.duration = DURATION.toLong()
        set.start()
    }

    /**
     * 手动添加拍照后的相片到图片列表，并设为选中
     *
     * @param media
     */
    private fun manualSaveFolder(media: LocalMedia) {
        try {
            createNewFolder(foldersList.toMutableList())
            val folder = getImageFolder(media.path, foldersList.toMutableList())
            val cameraFolder = if (foldersList.isNotEmpty()) foldersList[0] else null
            if (cameraFolder != null && folder != null) {
                // 相机胶卷
                cameraFolder.firstImagePath = media.path
                cameraFolder.images = foldersList[0].images
                cameraFolder.imageNum = cameraFolder.imageNum + 1
                // 拍照相册
                val num = folder.imageNum + 1
                folder.imageNum = num
                folder.images.add(0, media)
                folder.firstImagePath = cameraPath
                //folderWindow.bindFolder(foldersList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 将图片插入到相机文件夹中
     *
     * @param path
     * @param imageFolders
     * @return
     */
    private fun getImageFolder(path: String, imageFolders: MutableList<LocalMediaFolder>): LocalMediaFolder {
        val imageFile = File(path)
        val folderFile = imageFile.parentFile

        for (folder in imageFolders) {
            if (folder.name.equals(folderFile.name)) {
                return folder
            }
        }
        val newFolder = LocalMediaFolder()
        newFolder.name = folderFile.name
        newFolder.path = folderFile.absolutePath
        newFolder.firstImagePath = path
        imageFolders.add(newFolder)
        return newFolder
    }

    /**
     * 如果没有任何相册，先创建一个最近相册出来
     *
     * @param folders
     */
    private fun createNewFolder(folders: MutableList<LocalMediaFolder>) {
        if (folders.size == 0) {
            // 没有相册 先创建一个最近相册出来
            val newFolder = LocalMediaFolder()
            val folderName = if (config.mimeType === MediaMimeType.ofAudio())
                "所有音频"
            else
                "相机交卷"
            newFolder.name = folderName
            newFolder.path = ""
            newFolder.firstImagePath = ""
            folders.add(newFolder)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && PictureConfig.REQUEST_CAMERA == requestCode) {
            try {
                val file = File(cameraPath)
                //启动MediaScanner服务，扫描媒体文件
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //单选
            if (mSelectMode == PictureConfig.SINGLE) {
                singleRadioMediaImage()
            }

            // 生成新拍照片或视频对象
            val media = LocalMedia()
            val pictureType = MediaMimeType.createImageType(cameraPath)
            media.path = cameraPath
            media.pictureType = pictureType
            media.duration = 0
            media.isChecked = true
            media.mimeType = config.mimeType
            selectImages.add(media)

            adapter.addData(1, media)


            // 解决部分手机拍照完Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,不及时刷新问题手动添加
            //manualSaveFolder(media)

        }

    }


    private class Camera

}