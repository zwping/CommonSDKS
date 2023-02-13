package com.zwping.channel_push.receiver

import android.content.Context
import com.xiaomi.channel.commonutils.logger.LoggerInterface
import com.xiaomi.mipush.sdk.*
import com.zwping.channel_push.ChannelPush


/**
 *
 * 通过包名在 小米后台创建项目
 * 获取appId appKey通过 [init] 初始化
 *
 * 可感知消息的到达和点击 [onReceivePassThroughMessage]
 * [onNotificationMessageArrived] [onNotificationMessageClicked]
 *
 * zwping @ 2/23/21
 * https://dev.mi.com/console/doc/detail?pId=2625
 */
class MIReceiver : PushMessageReceiver() {

    companion object {

        fun register(ctx: Context?, miAPPID: String, miAPPKEY: String) {
            ChannelPush.logd("----mi push init --")
            MiPushClient.registerPush(ctx, miAPPID, miAPPKEY)
            Logger.setLogger(ctx, object : LoggerInterface {
                override fun setTag(tag: String) {
                }

                override fun log(content: String, t: Throwable) {
                    ChannelPush.logd("$content $t")
                }

                override fun log(content: String) {
                    ChannelPush.logd(content)
                }
            })
        }

        fun unregister(ctx: Context?) {
            ChannelPush.logd("----xiaomi push unregister --")
            MiPushClient.unregisterPush(ctx)
        }

    }

    override fun onReceivePassThroughMessage(context: Context?, message: MiPushMessage) { // 透传消息
        val mMessage = message.content
        ChannelPush.logd("小米透传消息到达----$mMessage (${message.topic}-${message.alias}-${message.userAccount})")
        /*
        when {
            !TextUtils.isEmpty(message.topic) -> mTopic = message.topic
            !TextUtils.isEmpty(message.alias) -> mAlias = message.alias
            !TextUtils.isEmpty(message.userAccount) -> mUserAccount = message.userAccount
        }
        */
    }

    override fun onNotificationMessageClicked(context: Context?, message: MiPushMessage) { // 通知栏点击
        val mMessage = message.content
        ChannelPush.logd("小米通知点击----$mMessage (${message.topic}-${message.alias}-${message.userAccount})$context")
        ChannelPush.onClickLis.invoke(context, mMessage)
        /*
        when {
            !TextUtils.isEmpty(message.topic) -> mTopic = message.topic
            !TextUtils.isEmpty(message.alias) -> mAlias = message.alias
            !TextUtils.isEmpty(message.userAccount) -> mUserAccount = message.userAccount
        }
        */
    }

    override fun onNotificationMessageArrived(context: Context?, message: MiPushMessage) { // 通知消息到达
        val mMessage = message.content
        ChannelPush.logd("小米通知到达----$mMessage (${message.topic}-${message.alias}-${message.userAccount})")
        ChannelPush.onArrivalLis.invoke(context, mMessage)
        /*
        when {
            !TextUtils.isEmpty(message.topic) -> mTopic = message.topic
            !TextUtils.isEmpty(message.alias) -> mAlias = message.alias
            !TextUtils.isEmpty(message.userAccount) -> mUserAccount = message.userAccount
        }
        */
    }

    override fun onCommandResult(context: Context?, message: MiPushCommandMessage) { // 指令响应
        val command = message.command
        val arguments = message.commandArguments
        val cmdArg1 = if (arguments != null && arguments.size > 0) arguments[0] else null
        ChannelPush.logd("小米指令响应----$command-$arguments-$cmdArg1")
        /*
        val cmdArg2 = if (arguments != null && arguments.size > 1) arguments[1] else null
        if (message.resultCode.toInt() == ErrorCode.SUCCESS) {
            when {
                MiPushClient.COMMAND_REGISTER == command ->
                    mRegId = cmdArg1
                MiPushClient.COMMAND_SET_ALIAS == command || MiPushClient.COMMAND_UNSET_ALIAS == command->
                    mAlias = cmdArg1
                MiPushClient.COMMAND_SUBSCRIBE_TOPIC == command || MiPushClient.COMMAND_UNSUBSCRIBE_TOPIC == command ->
                    mTopic = cmdArg1
                MiPushClient.COMMAND_SET_ACCEPT_TIME == command -> {
                    mStartTime = cmdArg1
                    mEndTime = cmdArg2
                }
            }
        }
        */
    }

    override fun onReceiveRegisterResult(context: Context?, message: MiPushCommandMessage) { // 注册响应
        val command = message.command
        val arguments = message.commandArguments
        val cmdArg1 = if (arguments != null && arguments.size > 0) arguments[0] else null
        if (message.resultCode.toInt() == ErrorCode.SUCCESS && MiPushClient.COMMAND_REGISTER == command) {
            val mRegId = cmdArg1
            ChannelPush.logd("小米push register suc $mRegId")
            ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Xiaomi, mRegId)
        }
        /*
        val cmdArg2 = if (arguments != null && arguments.size > 1) arguments[1] else null
         */
    }
}