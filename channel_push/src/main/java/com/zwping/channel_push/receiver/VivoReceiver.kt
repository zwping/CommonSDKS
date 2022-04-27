package com.zwping.channel_push.receiver

import android.content.Context
import com.vivo.push.PushClient
import com.vivo.push.sdk.OpenClientPushMessageReceiver
import com.zwping.channel_push.ChannelPush

/**
 *
 * vivo平台必须审核过后app才能正式推送
 * 在开发阶段需在vivo后台添加测试设备, 同时后台推送时restful api中params pushMode=1 (0: 正式推送 1: 测试推送)
 *
 * appId & appKey 在androidManifest中配置
 *
 * zwping @ 2/25/21
 */
class VivoReceiver : OpenClientPushMessageReceiver() {

    companion object {
        fun register(ctx: Context?) {
            ChannelPush.logd("----vivo push init --")
            PushClient.getInstance(ctx).initialize()
            PushClient.getInstance(ctx).turnOnPush {
                if (it != 0) return@turnOnPush  // 开关状态处理， 0代表成功
                val regId = PushClient.getInstance(ctx).regId
                ChannelPush.logd("VIVO push register suc $regId")
                ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Vivo, regId)
            }
        }
        fun unregister(ctx: Context?) {
            ChannelPush.logd("----vivo push unregister --")
            PushClient.getInstance(ctx).turnOffPush {
                if (it == 0) {
                    ChannelPush.logd("----vivo push unregister suc --")
                } else {
                    ChannelPush.logd("----vivo push unregister fail --")
                }
            }
        }
    }






}