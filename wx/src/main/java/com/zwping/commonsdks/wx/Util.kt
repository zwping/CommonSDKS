package com.zwping.commonsdks.wx

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*

/**
 *
 * zwping @ 2021/8/17
 */
internal object Util {

    const val TAG = "commonsdks.wx" // MicroMsg.SDK

    var app: Application? = null

    private var _isDebugMode : Boolean? = null
    fun isDebugMode(ctx: Context? = null): Boolean {
        if (ctx != null || app != null)
            _isDebugMode = (ctx ?: app!!).applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return _isDebugMode == true
    }

    fun toast(msg: Any?, ctx: Context? = null) {
        if (ctx == null && app == null) return
        Toast.makeText(ctx ?: app!!, "$msg", Toast.LENGTH_SHORT).show()
    }

    fun Any.log() { if(isDebugMode()) Log.v(TAG, "$this ") }

    fun getAppName(ctx: Context? = null): String {
        if (ctx == null && app == null) return TAG
        return try {
            val name = (ctx ?: app!!).packageName
            val pm = (ctx ?: app!!).packageManager
            val pi = pm.getPackageInfo(name, 0)
            pi.applicationInfo.loadLabel(pm).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            TAG
        }
    }

    fun getAppIcon(ctx: Context? = null): Bitmap? {
        if (ctx == null && app == null) return null
        return try {
            val name = (ctx ?: app!!).packageName
            val pm = (ctx ?: app!!).packageManager
            val pi = pm.getPackageInfo(name, 0)
            (pi.applicationInfo.loadIcon(pm) as BitmapDrawable).bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /*** 转换为安全的字符串, 中文占3字节 ***/
    fun String.safeStr(maxByte: Long) = if (trim().length > maxByte) substring(0, trim().length/3) else this

    /*** 高效获取Img ***/
    fun getImg(realPath: String?): Bitmap? {
        if (realPath == null) return null
        val newOpts: BitmapFactory.Options = BitmapFactory.Options()
        newOpts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(realPath, newOpts)
        newOpts.inJustDecodeBounds = false
        val w = newOpts.outWidth
        val h = newOpts.outHeight
        // 现在主流手机比较多是1280*720分辨率，所以高和宽我们设置为
        val hh = 1280F
        val ww = 720F
        var be = 1
        if (w > h && w > ww) {
            be = (newOpts.outWidth / ww).toInt()
        } else if (w < h && h > hh) {
            be = (newOpts.outHeight / hh).toInt()
        }
        if (be <= 0) be = 1
        newOpts.inSampleSize = be   //设置缩放比例
        return BitmapFactory.decodeFile(realPath, newOpts)
    }

    /*** 压缩图片 ***/
    fun compressBmp2ByteArray(bmp: Bitmap?, isTransparentImg: Boolean, limitByte: Long, needRecycle: Boolean): ByteArray? {
        if (null == bmp) return null
        val baos = ByteArrayOutputStream()
        var bmp2: Bitmap? = null
        try {
            // val format = Bitmap.CompressFormat.JPEG // 有损压缩，但会造成透明图层染黑
            val format = if (isTransparentImg) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            bmp.compress(format, 100, baos)
            if (baos.toByteArray().size < limitByte) return baos.toByteArray()
            val w = bmp.width
            val h = bmp.height
            val nw = 720F // 主流手机宽
            if (baos.toByteArray().size > limitByte * 2 && w > nw) { // 基于主流宽 如果bmp大于2倍的limit，则缩小尺寸进行压缩
                val matrix = Matrix().apply { postScale(nw / w, nw*h/w / h) }
                bmp2 = Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true)
                baos.reset()
                bmp2.compress(format, 100, baos)
            }
            val bmp3 = bmp2 ?: bmp
            var quality = 100
            while (baos.toByteArray().size > limitByte) {
                baos.reset()
                bmp3.compress(format, quality, baos)
                quality -= 10
                if (quality < 0) break
            }
            return baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                if (needRecycle) bmp.recycle()
                bmp2?.recycle()
                baos.close()
            } catch (e: Exception){ }
        }
    }

    /*** 是否有透明图层 png / gif ***/
    fun isTransparentImg(realPath: String?): Boolean {
        try {
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeFile(realPath, opts)
            val type = opts.outMimeType
            if (!type.isNullOrBlank()) return type == "image/png" || type == "image/gif"
            val path = URI(realPath).path
            if (!path.contains(".")) return false
            val p = path.split(".")
            val suffix = p[p.size-1].lowercase(Locale.getDefault())
            return suffix == "png" || suffix == "gif"
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /*** byteArray 2 bitmap ***/
    fun byteArray2Bmp(byteArray: ByteArray?): Bitmap? {
        if (byteArray == null) return null
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    /*** bitmap 2 byteArray ***/
    fun bmp2ByteArray(bmp: Bitmap?, needRecycle: Boolean): ByteArray? {
        if (null == bmp) return null
        val baos = ByteArrayOutputStream()
        return try {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                if (needRecycle) bmp.recycle()
                baos.close()
            } catch (e: Exception) { }
        }
    }

}