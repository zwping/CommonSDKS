package com.zwping.channel_push

import android.content.Context
import com.zwping.channel_push.receiver.Huawei
import com.zwping.channel_push.receiver.MIReceiver
import com.zwping.channel_push.receiver.Oppo
import com.zwping.channel_push.receiver.VivoReceiver

/**
 * zwping @ 2022/4/26
 * 借助tpns文档更容易理解渠道推送
 * https://cloud.tencent.com/document/product/548/57361
 */
object ChannelPush {

    enum class Channel{ Mi, Huawei, Oppo, Vivo }

    var logd: (msg: Any?) -> Unit = { }
    var onRegisterCallback: (channel: Channel, regId: String?) -> Unit = { _, _ -> }
    // 只有小米才走BR, 其余均走scheme uri
    var onArrivalLis: (app: Context?, data: String?) -> Unit = { _, _ -> }
    var onClickLis: (app: Context?, data: String?) -> Unit = { _, _ -> }

    /*** 注册推送 ***/
    fun register(
        ctx: Context?,
        miId: String, miKey: String,
        oppoKey: String, oppoSecret: String, )
    {
        when {
            RomUtils.isHuawei() -> Huawei.register(ctx)
            RomUtils.isXiaomi() -> MIReceiver.register(ctx, miId, miKey)
            RomUtils.isOppo() -> Oppo.register(ctx, oppoKey, oppoSecret)
            RomUtils.isVivo() -> VivoReceiver.register(ctx)
            else -> logd("非${Channel.values().map { it.name }}渠道")
        }
    }
    /*** 反注册推送 ***/
    fun unregister(ctx: Context?)
    {
        ctx ?: return
        when {
            RomUtils.isHuawei() -> Huawei.unregister(ctx)
            RomUtils.isXiaomi() -> MIReceiver.unregister(ctx)
            RomUtils.isOppo() -> Oppo.unregister(ctx)
            RomUtils.isVivo() -> VivoReceiver.unregister(ctx)
        }
    }
    /*** 设置角标 ***/
    fun setBadgeNum(num: Int)
    {

    }
    /*** 清除通知栏 ***/
    fun clearAllNotify()
    {

    }
}