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
import com.zwping.commonsdks.wx.Util.app
import com.zwping.commonsdks.wx.Util.getImg
import com.zwping.commonsdks.wx.wxapi.WXPayEntryActivity
import kotlin.random.Random

/**
 * 微信sdk扩展
 * [WxHelper.init] 初始化wx sdk, 注意隐私协议
 * [WxHelper.shareMini] 分享小程序
 * [WxHelper.shareImg]  分享图片
 * [WxHelper.shareTxt]  分享文本
 * [WxHelper.login]     微信登录
 * [WxHelper.pay]       微信支付
 * zwping @ 2021/8/17
 */
object WxHelper: IWxHelper {

    override var api: IWXAPI? = null

    /**
     * 初始化wx sdk, 注意隐私协议
     * @param wxAppId BuildConfig.WX_ID | build.gradle->defaultConfig { buildConfigField "String", "WX_ID", '"xxx"' }
     */
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

    override fun login(lis: OnLoginListener) {
        if (api == null) { lis.callback(false, null, -101, ConstArgsErr); return }
        WXEntryActivity.wxLoginLis = WeakReference(lis)
        val r = api?.sendReq(SendAuth.Req().apply { scope = "snsapi_userinfo" })
        if (r != true) lis.callback(false, null, -100, ConstCodeExtra3)
    }

    override fun pay(lis: OnPayListener,
                     partnerId: String?, prepayId: String?, packageValue: String?,
                     nonceStr: String?, timeStamp: String?, sign: String?) {
        // createWxApi(ctx, appId)
        if (api == null) { lis.callback(false, null, -100, ConstArgsErr); return }
        WXPayEntryActivity.wxPayListener = WeakReference(lis)
        val r = api?.sendReq(PayReq().also {
            it.appId = WxAppId; it.partnerId = partnerId; it.prepayId = prepayId
            it.packageValue = packageValue; it.nonceStr = nonceStr
            it.timeStamp = timeStamp; it.sign = sign
        })
        if (r == false) lis.callback(false, null, -101, ConstCodeExtra4)
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
        // val data = compressBmp2ByteArray(bmp, isTransparentImg(realPath), 26214400, false)
        // 根据https://developers.weixin.qq.com/community/minihome/doc/0006225b1749c81b25bac22e15f400
        // 提示，imageData 限制500kb合适, 如果传更高清图, 需使用imagePath (imagePath对于双开微信不支持, 且微信官方不回复相关问题解决方案)
        // Binder传输机制限制了imageData Byte[] 大小
        val data = compressBmp2ByteArray(bmp, isTransparentImg(realPath), 500*1024L, false)
        entity.imgObj = WXImageObject(data)
        entity.thumbData = bmp2ByteArray(Bitmap.createScaledBitmap(bmp, 150, 150, true), 32*1024L, true)
        bmp.recycle()
        sendReq(scene, entity, lis, ctx, appId)
    }
    override fun shareImg(scene: WxScene, realPath: String?, lis: OnShareListener) { shareImg(scene, realPath, lis, null, null) }

    /**
     *
     */
    fun shareMini(entity: WxMiniEntity, lis: OnShareListener) {
        val bmp = getImg(entity.imgLocalPath)
        if (null == bmp) { lis.callback(false, -100, ConstCodeExtra1); return }
        WXEntryActivity.wxShareLis = WeakReference(lis)
        val data = compressBmp2ByteArray(bmp, isTransparentImg(entity.imgLocalPath), 128*1024L, false)
        val r = WxHelper.api?.sendReq(entity.req(data))
        if (r == false) { lis.callback(false, -100, ConstCodeExtra2) }
        bmp.recycle()
    }

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

    override fun safeSts(str: String, maxByte: Long): String =
        str.safeStr(maxByte)
    override fun getImg(realPath: String?): Bitmap? =
        Util.getImg(realPath)
    override fun compressBmp2ByteArray(bmp: Bitmap?, isTransparentImg: Boolean, limitByte: Long, needRecycle: Boolean): ByteArray? =
        Util.compressBmp2ByteArray(bmp, isTransparentImg, limitByte, needRecycle)
    override fun isTransparentImg(realPath: String?): Boolean =
        Util.isTransparentImg(realPath)
    override fun byteArray2Bmp(byteArray: ByteArray?): Bitmap? =
        Util.byteArray2Bmp(byteArray)
    override fun bmp2ByteArray(bmp: Bitmap?, limitByte: Long, needRecycle: Boolean): ByteArray? =
        Util.compressBmp2ByteArray(bmp, false, limitByte, needRecycle)

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
            return "WxMsgEntity(title=$title, " +
                    "description=$description, " +
                    "thumbData=${thumbData?.size}, " +
                    "mediaMsg=$mediaMsg, " +
                    "textObj=$textObj, " +
                    "imgObj=${imgObj?.imageData?.size ?: imgObj?.imagePath}, " +
                    "musicObj=$musicObj, " +
                    "videoObj=$videoObj, " +
                    "webObj=$webObj, " +
                    "fileObj=$fileObj, " +
                    "extendObj=$extendObj, " +
                    "miniObj=$miniObj)"
        }
    }
    class WxMiniEntity {

        var webpageUrl = "https://weixin.com"   // 兼容低版本的网页链接
        var miniType = 0                        // 正式版:0，测试版:1，体验版:2
        var miniId: String? = null              // 小程序原始id gh_d43f693ca31f
        var path: String? = null                //小程序页面路径；对于小游戏，可以只传入 query 部分，来实现传参效果，如：传入 "?foo=bar"
        var title: String? = null               // 小程序消息title
        var desc: String? = null                // 小程序消息desc
        var imgLocalPath: String? = null        // 小程序消息封面图片，小于128k

        constructor()
        constructor(miniType: Int, miniId: String?, path: String?, title: String?, imgLocalPath: String?) {
            this.miniType = miniType
            this.miniId = miniId
            this.path = path
            this.title = title
            this.imgLocalPath = imgLocalPath
        }

        fun req(thumbData: ByteArray?) = SendMessageToWX.Req().also { req ->
            req.transaction = "mini${System.currentTimeMillis()}${Random.nextInt(0, 100)}" // 事务id, 与resp呼应使用
            req.scene = SendMessageToWX.Req.WXSceneSession // 目前只支持会话
            val miniObj = WXMiniProgramObject().also {
                it.webpageUrl = webpageUrl
                it.miniprogramType = miniType
                it.userName = miniId
                it.path = path
            }
            req.message = WXMediaMessage(miniObj).also {
                it.title = title
                it.description = desc
                it.thumbData = thumbData
            }
        }
    }

    enum class WxScene(val value: Int) {
        Friend(WXSceneSession),     // 对话
        Moments(WXSceneTimeline),   // 朋友圈
        Favorite(WXSceneFavorite),  // 收藏
    }

    // 公开常量, 可随意替换
    var ConstRegisterFail   = "应用AppId注册到微信失败"
    var ConstWxUnInstall    = "未安装微信"
    var ConstArgsErr        = "args error"
    var ConstCodeMsg1       = "分享成功"
    var ConstCodeMsg2       = "分享取消"
    var ConstCodeMsg3       = "分享被拒绝"
    var ConstCodeMsg4       = "分享不支持的错误"
    var ConstCodeMsg5       = "分享返回"
    var ConstCodeMsg6       = "登录成功"
    var ConstCodeMsg7       = "用户拒绝授权"
    var ConstCodeMsg8       = "用户取消"
    var ConstCodeMsg9       = "登录失败"
    var ConstCodeMsg10      = "支付成功"
    var ConstCodeMsg11      = "支付错误"
    var ConstCodeMsg12      = "支付取消"
    var ConstCodeExtra1     = "图片为空"
    var ConstCodeExtra2     = "分享数据发送失败"
    var ConstCodeExtra3     = "登录数据发送失败"
    var ConstCodeExtra4     = "支付数据发送失败"
}