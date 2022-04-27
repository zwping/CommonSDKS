package com.zwping.channel_push.receiver

import android.content.Context
import com.heytap.msp.push.HeytapPushManager
import com.heytap.msp.push.callback.ICallBackResultService
import com.zwping.channel_push.ChannelPush

/**
 * oppo推送
 * zwping @ 2/25/21
 */
object Oppo {

    fun register(ctx: Context?, key: String, secret: String) {
        ChannelPush.logd("----oppo push init --")
        HeytapPushManager.init(ctx, false)
        if (!HeytapPushManager.isSupportPush(ctx)) {
            ChannelPush.logd("Oppo push isSupportPush no ")
            return
        }
        // val regId = HeytapPushManager.getRegisterID()
        HeytapPushManager.register(ctx, key, secret, object: ICallBackResultService{
            override fun onRegister(p0: Int, p1: String?) {
                ChannelPush.logd("Oppo push register suc $p0 $p1")
                ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Oppo, p1)
            }
            override fun onUnRegister(p0: Int) {
                ChannelPush.logd("oppo push unregister $p0")
            }
            override fun onSetPushTime(p0: Int, p1: String?) {
                ChannelPush.logd("oppo push 获取设置推送时间的执行结果 $p0 $p1")
            }
            override fun onGetPushStatus(p0: Int, p1: Int) {
                ChannelPush.logd("oppo push 获取当前的push状态 $p0 $p1")
            }
            override fun onGetNotificationStatus(p0: Int, p1: Int) {
                ChannelPush.logd("oppo push 当前通知栏状 $p0 $p1")
            }
            override fun onError(p0: Int, p1: String?) {
                ChannelPush.logd("oppo push 错误码 $p0 $p1")
            }
        })
    }

    fun unregister(ctx: Context?) {
        ChannelPush.logd("oppo push unregister")
        HeytapPushManager.unRegister()
    }

    fun clearNotify() {
        HeytapPushManager.clearNotifications()
    }

    // 弹出通知栏权限弹窗（仅一次）
    fun requestNotifyPermission() {
        HeytapPushManager.requestNotificationPermission()
    }

    fun openNotifySetting() {
        HeytapPushManager.openNotificationSettings()
    }

}