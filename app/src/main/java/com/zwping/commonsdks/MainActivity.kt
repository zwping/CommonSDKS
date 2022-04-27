package com.zwping.commonsdks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.zwping.channel_push.ChannelPush
import com.zwping.channel_push.receiver.MIReceiver
import com.zwping.commonsdks.wx.IWxHelper
import com.zwping.commonsdks.wx.WxHelper

class MainActivity : AppCompatActivity() {

    companion object {
        val WXID = /* BuildConfig.WX_ID */ "wx8b49d9e2dbefb953"
    }

    private val handler by lazy { Handler(Looper.myLooper()!!) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        WxHelper.init(application, WXID)
//        WxHelper.shareTxt(WxHelper.WxScene.Friend, "123", object: IWxHelper.OnShareListener {
//            override fun callback(state: Boolean, errCode: Int, msg: String) {
//            }
//        })

        ChannelPush.logd = { println(it) }
        ChannelPush.register(this,
            "2882303761520153635", "5112015384635",
            "91615b45763d4d169af6624eff875760", "fef1fdeb70e34c208326d30abfda4b31",
        )
        MIReceiver.register(this, "2882303761520153635", "5112015384635")

//        handler.postDelayed({ ChannelPush.unregister(this) }, 2000)
    }
}