package com.zwping.commonsdks.wx.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.zwping.commonsdks.wx.IWxHelper
import com.zwping.commonsdks.wx.Util.log
import com.zwping.commonsdks.wx.WxHelper
import java.lang.ref.WeakReference

/**
 * 微信登录 [WxHelper.login]
 * 微信分享 [WxHelper.shareTxt] [WxHelper.shareImg]
 * 接收微信登录/分享回调      [onResp]
 * 未完善微信调取第三方app功能 [onReq]
 * zwping @ 2021/8/17
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { WxHelper.api?.handleIntent(intent, this) }
        catch (e: Exception) { e.printStackTrace() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        WxHelper.api?.handleIntent(intent, this)
    }

    override fun onDestroy() {
        wxShareLis = null
        wxLoginLis = null
        super.onDestroy()
    }

    /**
     * sendReq的响应结果 ( 没有失败 )
     * https://docs.msdk.qq.com/v5/zh-CN/FAQS/59d29a3663b5647d4bc339fd3fa195aa/7fd95b8b879ed11b2775edf0acd1feeb.html
     */
    override fun onResp(p0: BaseResp?) {
        val errCode = p0?.errCode ?: BaseResp.ErrCode.ERR_BAN
        val msg = when(errCode) {
            BaseResp.ErrCode.ERR_OK -> WxHelper.ConstCodeMsg1
            BaseResp.ErrCode.ERR_USER_CANCEL -> WxHelper.ConstCodeMsg2
            BaseResp.ErrCode.ERR_AUTH_DENIED -> WxHelper.ConstCodeMsg3
            BaseResp.ErrCode.ERR_UNSUPPORT -> WxHelper.ConstCodeMsg4
            else -> WxHelper.ConstCodeMsg5
        }
        val msg2 = when(errCode) {
            BaseResp.ErrCode.ERR_OK -> WxHelper.ConstCodeMsg6
            BaseResp.ErrCode.ERR_AUTH_DENIED -> WxHelper.ConstCodeMsg7
            BaseResp.ErrCode.ERR_USER_CANCEL -> WxHelper.ConstCodeMsg8
            else -> WxHelper.ConstCodeMsg9
        }
        "WXEntryActivity onResp $msg $msg2 ${p0?.errCode} ${p0?.errStr} ${p0?.openId} ${p0?.transaction} ${p0?.type} ${p0?.checkArgs()} ${wxShareLis?.get()}".log()

        val state = errCode == BaseResp.ErrCode.ERR_OK
        wxShareLis?.get()?.callback(state, errCode, msg)
        wxLoginLis?.get()?.callback(state && p0 is SendAuth.Resp, (p0 as SendAuth.Resp).code, errCode, msg2)
        finish()
    }

    /*** sendResp的响应结果 ***/
    override fun onReq(p0: BaseReq?) {
        "WXEntryActivity onReq ${p0?.openId} ${p0?.transaction}".log()
        // finish()
    }

    companion object {
        var wxShareLis: WeakReference<IWxHelper.OnShareListener>? = null
        var wxLoginLis: WeakReference<IWxHelper.OnLoginListener>? = null
    }

}