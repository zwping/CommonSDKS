package com.zwping.channel_push.receiver

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.RemoteMessage
import com.zwping.channel_push.ChannelPush

/**
 *
 * 基于HMS生态, 配置略显复杂
 * 通过包名(同时需要设置SHA-256)在 华为后台创建项目，获取agconnect-service.json
 * agc插件会初始化agconnect-service.json, 通过[init]初始化
 *
 * 在根build.gradle设置 hms仓库地址 和 agc插件地址
 *      maven {url 'https://developer.huawei.com/repo/'}
 *      classpath 'com.huawei.agconnect:agcp:1.4.2.300'
 * 在主工程模块(app module)中引入agc插件
 *      apply plugin: 'com.huawei.agconnect'
 * 下载agconnect-service.json置于主工程模块根目录下
 *
 * 可感知透传消息到达 [onMessageReceived]
 * 通知栏点击通过android scheme约定跳转
 *
 * zwping @ 2/24/21
 */
object Huawei {

    fun register(ctx: Context?) {
        ChannelPush.logd("----huawei push init --")
        Thread{
            try {
                // 从agconnect-service.json文件中读取appId
                val appId = AGConnectServicesConfig.fromContext(ctx).getString("client/app_id")
                val token = HmsInstanceId.getInstance(ctx).getToken(appId, "HCM")
                ChannelPush.logd("华为push register suc $token")
                ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Huawei, token)
            } catch (e: ApiException) {
                e.printStackTrace()
                ChannelPush.logd("华为push register fail $e")
            }
        }.start()
    }

    fun unregister(ctx: Context?) {
        Thread{
            try {
                val appId = AGConnectServicesConfig.fromContext(ctx).getString("client/app_id")
                HmsInstanceId.getInstance(ctx).deleteToken(appId, "HCM")
                ChannelPush.logd("华为push unregister suc")
            } catch (e: ApiException) {
                e.printStackTrace()
                ChannelPush.logd("华为push unregister fail $e")
            }
        }.start()
    }

    /*** 设置角标 ***/
    fun setUnReadNum(ctx: Context?, num: Int) {
        try {
            ctx?.contentResolver?.call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), "change_badge", null,
                Bundle().apply {
                    val it = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                    putString("package", it?.component?.packageName)
                    putString("class", it?.component?.className)
                    putInt("badgenumber", num)
                })
        } catch (e:Exception) { }
    }

}

class HmsMessageService: com.huawei.hms.push.HmsMessageService(){
    override fun onMessageReceived(p0: RemoteMessage?) { // 透传消息
        super.onMessageReceived(p0)
        ChannelPush.logd("华为透传消息到达----$p0")
    }

    override fun onNewToken(p0: String?) {
        super.onNewToken(p0)
        ChannelPush.logd("华为token refresh $p0")
        ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Huawei, p0)
    }

    override fun onNewToken(p0: String?, p1: Bundle?) {
        super.onNewToken(p0, p1)
        ChannelPush.logd("华为token refresh2 $p0 $p1")
        ChannelPush.onRegisterCallback.invoke(ChannelPush.Channel.Huawei, p0)
    }
}