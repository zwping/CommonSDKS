package com.zwping.commonsdks.wx

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.tencent.mm.opensdk.modelmsg.*
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX.Req.*
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.zwping.commonsdks.wx.Util.log
import com.zwping.commonsdks.wx.Util.safeStr
import com.zwping.commonsdks.wx.wxapi.WXEntryActivity
import java.lang.ref.WeakReference
import java.util.*
import com.zwping.commonsdks.wx.IWxHelper.*
import com.zwping.commonsdks.wx.wxapi.WXPayEntryActivity

/**
 * 微信sdk扩展
 *      分享
 *      支付
 * zwping @ 2021/8/17
 */
object WxHelper: IWxHelper {

    override var api: IWXAPI? = null

    override fun init(app: Application, wxAppId: String) {
        Util.app = app
        createWxApi(app, wxAppId)
    }

    /*** AppId ***/
    var WxAppId: String? = null
        set(value) {
            if (!value.isNullOrBlank()) field = value
        }
        get() {
            if (Util.isDebugMode() && field.isNullOrBlank()) throw IllegalArgumentException("未写入wx app id")
            return field
        }

    private fun createWxApi(ctx: Context?, appId: String?) {
        if(api != null) return
        WxAppId = appId
        api = WXAPIFactory.createWXAPI(ctx ?: Util.app, WxAppId, true) // true == 校验签名
        if (api?.isWXAppInstalled == true && api?.registerApp(WxAppId) != true)
            Util.toast(ConstRegisterFail, ctx)
    }

    override fun login(lis: OnLoginListener, ctx: Context?, appId: String?) {
        createWxApi(ctx, appId)
        if (api == null) { lis.callback(false, null, -101, ConstArgsErr); return }
        WXEntryActivity.wxLoginLis = WeakReference(lis)
        val r = api?.sendReq(SendAuth.Req().apply { scope = "snsapi_userinfo" }) == true
        if (!r) lis.callback(false, null, -100, ConstCodeExtra3)
    }

    override fun pay(lis: OnPayListener,
                     appId: String?, partnerId: String?, prepayId: String?, packageValue: String?,
                     nonceStr: String?, timeStamp: String?, sign: String?, ctx: Context?) {
        createWxApi(ctx, appId)
        if (api == null) { lis.callback(false, null, -100, ConstArgsErr); return }
        WXPayEntryActivity.wxPayListener = WeakReference(lis)
        val r = api?.sendReq(PayReq().also {
            it.appId = appId; it.partnerId = partnerId; it.prepayId = prepayId
            it.packageValue = packageValue; it.nonceStr = nonceStr
            it.timeStamp = timeStamp; it.sign = sign
        }) == true
        if (!r) lis.callback(false, null, -101, ConstCodeExtra4)
    }

    override fun shareTxt(scene: WxScene, txt: String, lis: OnShareListener, ctx: Context?, appId: String?) {
        val entity = WxMsgEntity()
        entity.description = txt
        entity.textObj = WXTextObject().apply { text = txt }
        sendReq(scene, entity, lis, ctx, appId)
    }
    override fun shareTxt(scene: WxScene, txt: String, lis: OnShareListener) { shareTxt(scene, txt, lis, null, null) }

    override fun shareImg(scene: WxScene, realPath: String?, lis: OnShareListener, ctx: Context?, appId: String?) {
        val bmp = getImg(realPath)
        if (null == bmp) {
            lis.callback(false, -100, ConstCodeExtra1); return
        }
        val entity = WxMsgEntity()
        val data = compressBmp2ByteArray(bmp, isTransparentImg(realPath), 26214400, false)
        entity.imgObj = WXImageObject(data)
        entity.thumbData = bmp2ByteArray(Bitmap.createScaledBitmap(bmp, 150, 150, true), true)
        bmp.recycle()
        sendReq(scene, entity, lis, ctx, appId)
    }
    override fun shareImg(scene: WxScene, realPath: String?, lis: OnShareListener) { shareImg(scene, realPath, lis, null, null) }

    override fun sendReq(scene: WxScene, entity: WxMsgEntity, lis: OnShareListener, ctx: Context?, appId: String?) {
        "sendReq $scene $entity $ctx $appId".log()
        createWxApi(ctx, appId)
        if (api == null) { lis.callback(false, -101, ConstArgsErr); return }
        if (api?.isWXAppInstalled == false) { lis.callback(false, -100, ConstWxUnInstall); return }

        val msg = entity.mediaMsg ?: WXMediaMessage()
        entity.title?.also { msg.title = it.safeStr(WXMediaMessage.TITLE_LENGTH_LIMIT.toLong()) }                       // <= 512Bytes
        entity.description?.also { msg.description = it.safeStr(WXMediaMessage.DESCRIPTION_LENGTH_LIMIT.toLong()) }     // <= 1KB
        entity.thumbData?.also { msg.thumbData = it }                       // <= 32KB

        entity.textObj?.also { msg.mediaObject = it.apply {
            text = text.safeStr(10 * 1024)
        } }
        entity.imgObj?.also { msg.mediaObject = it }
        entity.musicObj?.also { msg.mediaObject = it }
        entity.videoObj?.also { msg.mediaObject = it }
        entity.webObj?.also { msg.mediaObject = it }
        entity.fileObj?.also { msg.mediaObject = it }
        entity.extendObj?.also { msg.mediaObject = it }
        entity.miniObj?.also { msg.mediaObject = it }

        if (msg.mediaObject?.checkArgs() == false) {
            lis.callback(false, -100, "$ConstArgsErr - ${msg.mediaObject?.type()}"); return
        }

        val t = System.currentTimeMillis()
        val transaction = when(msg.mediaObject) {
            is WXMiniProgramObject -> "mini$t"
            is WXAppExtendObject -> "extend$t"
            is WXFileObject -> "file$t"
            is WXWebpageObject -> "webpage$t"
            is WXVideoObject -> "video$t"
            is WXMusicObject -> "music$t"
            is WXImageObject -> "img$t"
            is WXTextObject -> "text$t"
            else -> "$t"
        }

        val req = SendMessageToWX.Req()
        req.transaction = transaction       // 事务ID
        req.scene = scene.value
        req.message = msg


        WXEntryActivity.wxShareLis = WeakReference(lis)
        val r = api?.sendReq(req) == true
        if (!r) { lis.callback(false, -100, ConstCodeExtra2) }
    }
    override fun sendReq(scene: WxScene, entity: WxMsgEntity, lis: OnShareListener) = sendReq(scene, entity, lis, null, null)

    override fun safeSts(str: String, maxByte: Long): String = str.safeStr(maxByte)
    override fun getImg(realPath: String?): Bitmap? = Util.getImg(realPath)
    override fun compressBmp2ByteArray(bmp: Bitmap?, isTransparentImg: Boolean, limitByte: Long, needRecycle: Boolean): ByteArray? = Util.compressBmp2ByteArray(bmp, isTransparentImg, limitByte, needRecycle)
    override fun isTransparentImg(realPath: String?): Boolean = Util.isTransparentImg(realPath)
    override fun byteArray2Bmp(byteArray: ByteArray?): Bitmap? = Util.byteArray2Bmp(byteArray)
    override fun bmp2ByteArray(bmp: Bitmap?, needRecycle: Boolean): ByteArray? = Util.bmp2ByteArray(bmp, needRecycle)

    // ============== 一些常量值 =================
    /**
     * 文本
     * 图片
     * 音乐
     * 视频
     * 网页
     */
    class WxMsgEntity {
        var title: String? = null                   // <= 512Bytes
        var description: String? = null             // <= 1KB
        var thumbData: ByteArray? = null            // <= 32KB

        var mediaMsg: WXMediaMessage? = null

        var textObj: WXTextObject? = null           // <= 10KB
        var imgObj: WXImageObject? = null           // imageData <= 10MB / imagePath <= 10MB
        var musicObj: WXMusicObject? = null         //
        var videoObj: WXVideoObject? = null
        var webObj: WXWebpageObject? = null
        var fileObj: WXFileObject? = null
        var extendObj: WXAppExtendObject? = null
        var miniObj: WXMiniProgramObject? = null

        override fun toString(): String {
            return "WxMsgEntity(title=$title, description=$description, thumbData=${thumbData?.contentToString()}, mediaMsg=$mediaMsg, textObj=$textObj, imgObj=$imgObj, musicObj=$musicObj, videoObj=$videoObj, webObj=$webObj, fileObj=$fileObj, extendObj=$extendObj, miniObj=$miniObj)"
        }

    }

    enum class WxScene(val value: Int) {
        Friend(WXSceneSession),     // 对话
        Moments(WXSceneTimeline),   // 朋友圈
        Favorite(WXSceneFavorite),  // 收藏
    }

    const val ConstRegisterFail = "应用AppId注册到微信失败"
    const val ConstWxUnInstall = "未安装微信"
    const val ConstArgsErr = "args error"
    const val ConstCodeMsg1 = "分享成功"
    const val ConstCodeMsg2 = "分享取消"
    const val ConstCodeMsg3 = "分享被拒绝"
    const val ConstCodeMsg4 = "分享不支持的错误"
    const val ConstCodeMsg5 = "分享返回"
    const val ConstCodeMsg6 = "登录成功"
    const val ConstCodeMsg7 = "用户拒绝授权"
    const val ConstCodeMsg8 = "用户取消"
    const val ConstCodeMsg9 = "登录失败"
    const val ConstCodeMsg10 = "支付成功"
    const val ConstCodeMsg11 = "支付错误"
    const val ConstCodeMsg12 = "支付取消"
    const val ConstCodeExtra1 = "图片为空"
    const val ConstCodeExtra2 = "分享数据发送失败"
    const val ConstCodeExtra3 = "登录数据发送失败"
    const val ConstCodeExtra4 = "支付数据发送失败"
}