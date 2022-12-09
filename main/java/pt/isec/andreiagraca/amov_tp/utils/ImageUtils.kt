package pt.isec.ans.rascunhos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

object ImageUtils {

    var imagePath : String? = null
    var nomeJogador:String?=null
    var bitm: Bitmap?=null

    fun setPic(view: View, path: String) {
        val targetW = view.width
        val targetH = view.height
        if (targetH < 1 || targetW < 1)
            return

        val bmpOptions = BitmapFactory.Options()
        bmpOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, bmpOptions)
        val photoW = bmpOptions.outWidth
        val photoH = bmpOptions.outHeight
        val scale = max(1,min(photoW / targetW, photoH / targetH))

        bmpOptions.inSampleSize = scale
        bmpOptions.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(path, bmpOptions)
        bitm=bitmap

        when {
            view is ImageView -> (view as ImageView).setImageBitmap(bitmap)
            //else -> view.background = bitmap.toDrawable(view.resources)
            else -> view.background = BitmapDrawable(view.resources,bitmap)
        }
    }

    fun setBitmap(view: View){
        if(bitm!=null) {
            when {
                view is ImageView -> (view as ImageView).setImageBitmap(bitm)
                //else -> view.background = bitmap.toDrawable(view.resources)
                else -> view.background = BitmapDrawable(view.resources, bitm)
            }
        }
    }

    fun setBitmap(view: View,bitmap:Bitmap){
        if(bitmap!=null) {
            when {
                view is ImageView -> (view as ImageView).setImageBitmap(bitmap)
                //else -> view.background = bitmap.toDrawable(view.resources)
                else -> view.background = BitmapDrawable(view.resources, bitmap)
            }
        }
    }
}