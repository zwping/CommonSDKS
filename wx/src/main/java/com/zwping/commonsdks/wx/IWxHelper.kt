package com.zwping.commonsdks.wx

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.tencent.mm.opensdk.openapi.IWXAPI

/**
 *
 * zwping @ 2021/8/18
 */
interface IWxHelper {

    /**
     * IWXAPI实例
     * [IWXAPI.isWXAppInstalled]        微信是否安装
     * [IWXAPI.openWXApp]               打开微信
     * [IWXAPI.sendReq]                 第三方 app 主动发送消息给微信，发送完成之后会切回到第三方 app 界面
     * [IWXAPI.sendResp]                微信向第三方 app 请求数据，第三方 app 回应数据之后会切回到微信界面
     */
    var api: IWXAPI?

    /**
     * 初始化<可选>
     * @param app
     * @param wxAppId
     */
    fun init(app: Application, wxAppId: String)

    fun login(lis: OnLoginListener, ctx: Context? = null, appId: String? = null)
    fun pay(lis: OnPayListener, appId: String?, partnerId: String?, prepayId: String?, packageValue: String?,
            nonceStr: String?, timeStamp: String?, sign: String?, ctx: Context? = null)

    fun shareTxt(scene: WxHelper.WxScene, txt: String, lis: OnShareListener, ctx: Context? = null, appId: String? = null)
    fun shareTxt(scene: WxHelper.WxScene, txt: String, lis: OnShareListener)

    fun shareImg(scene: WxHelper.WxScene, realPath: String?, lis: OnShareListener, ctx: Context? = null, appId: String? = null)
    fun shareImg(scene: WxHelper.WxScene, realPath: String?, lis: OnShareListener)

    /**
     * 第三方app向微信发送数据
     * @param scene         目标场景
     * @param entity        消息体 - 根据微信文档规则自动解析
     * @param lis           回调
     * @param ctx           <可选>
     * @param appId         <可选>
     */
    fun sendReq(scene: WxHelper.WxScene, entity: WxHelper.WxMsgEntity, lis: OnShareListener, ctx: Context? = null, appId: String? = null)
    fun sendReq(scene: WxHelper.WxScene, entity: WxHelper.WxMsgEntity, lis: OnShareListener)

    // ============== 工具 =================
    /**
     * 转换为安全字符串
     * @param str
     * @param maxByte unit - Byte
     */
    fun safeSts(str: String, maxByte: Long): String

    // 高效读取/压缩图片 https://jianshu.com/p/0b4854aae105
    /**
     * 获取图片bmp, 采样率压缩
     * @param realPath 图片真实路径
     * @return Bitmap?
     */
    fun getImg(realPath: String?): Bitmap?
    /**
     * Bitmap压缩 - 缩放 质量
     * @param bmp
     * @param isTransparentImg 是否为透明图层 Type.JPEG有损压缩兼容性更好，但透明图层会产生黑底
     * @param limitByte 压缩阀值 Byte
     * @param needRecycle
     */
    fun compressBmp2ByteArray(bmp: Bitmap?, isTransparentImg: Boolean, limitByte: Long, needRecycle: Boolean): ByteArray?
    /**
     * 是否有透明图层 png/gif
     * @param realPath 图片真实路径
     * @return Boolean
     */
    fun isTransparentImg(realPath: String?): Boolean

    fun byteArray2Bmp(byteArray: ByteArray?): Bitmap?
    fun bmp2ByteArray(bmp: Bitmap?, limitByte: Long, needRecycle: Boolean): ByteArray?

    fun interface OnShareListener{ fun callback(state: Boolean, errCode: Int, msg: String) }
    fun interface OnLoginListener{ fun callback(state: Boolean, code: String?, errCode: Int, msg: String) }
    fun interface OnPayListener{ fun callback(state: Boolean, extData: String?, errCode: Int, msg: String) }
}