package com.zwping.commonsdks.wx.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelpay.PayResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.zwping.commonsdks.wx.IWxHelper
import com.zwping.commonsdks.wx.Util.log
import com.zwping.commonsdks.wx.WxHelper
import java.lang.ref.WeakReference

/**
 * 微信支付 [WxHelper.pay]
 * 微信支付回调 [WXPayEntryActivity.onReq]
 * zwping @ 2021/8/17
 */
class WXPayEntryActivity : Activity(), IWXAPIEventHandler {

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
        wxPayListener = null
        super.onDestroy()
    }

    override fun onResp(p0: BaseResp?) {
        val errCode = p0?.errCode ?: BaseResp.ErrCode.ERR_COMM
        val msg = when(errCode) {
            BaseResp.ErrCode.ERR_OK -> WxHelper.ConstCodeMsg10
            BaseResp.ErrCode.ERR_USER_CANCEL -> WxHelper.ConstCodeMsg12
            else -> WxHelper.ConstCodeMsg11
        }
        val state = errCode == BaseResp.ErrCode.ERR_OK
        val payResp = if (p0 is PayResp) p0 else null
        wxPayListener?.get()?.callback(state, payResp?.extData, errCode, msg)
        "WXPayEntryActivity  onResp $msg ${p0?.errCode} ${payResp?.extData} ${p0?.errStr} ${p0?.openId} ${p0?.transaction} ${p0?.type} ${p0?.checkArgs()} ${WXEntryActivity.wxShareLis?.get()}".log()
        finish()
    }

    override fun onReq(p0: BaseReq?) {
        "WXPayEntryActivity onReq ${p0?.openId} ${p0?.transaction}".log()
        // finish()
    }

    companion object {
        var wxPayListener: WeakReference<IWxHelper.OnPayListener>? = null
    }
}