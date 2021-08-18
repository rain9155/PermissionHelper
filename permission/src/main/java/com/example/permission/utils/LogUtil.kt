package com.example.permission.utils

import android.util.Log
import com.example.permission.BuildConfig

/**
 * 打印log工具类
 * Created by 陈健宇 at 2021/8/14
 */
internal object LogUtil {

    private val isDebug = BuildConfig.DEBUG

    fun d(tag: String, msg: String){
        if(isDebug){
            Log.d(tag, msg)
        }
    }

    fun e(tag: String, msg: String){
        Log.e(tag, msg)
    }

}