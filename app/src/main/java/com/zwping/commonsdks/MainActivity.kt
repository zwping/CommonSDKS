package com.zwping.commonsdks

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.zwping.commonsdks.wx.IWxHelper
import com.zwping.commonsdks.wx.WxHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        WxHelper.init(application, "122223")
        WxHelper.shareTxt(WxHelper.WxScene.Friend, "123", object: IWxHelper.OnShareListener {
            override fun callback(state: Boolean, errCode: Int, msg: String) {
            }
        })
    }
}