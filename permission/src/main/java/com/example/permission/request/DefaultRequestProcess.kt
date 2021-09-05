package com.example.permission.request

import com.example.permission.IRequestCallback
import com.example.permission.base.IChain
import com.example.permission.base.REASON_REQUEST_CALLBACK
import com.example.permission.utils.LogUtil

/**
 * [IRequestCallback.IRequestProcess]的默认实现
 * Created by 陈健宇 at 2021/8/16
 */
internal class DefaultRequestProcess(private val chain: IChain) : IRequestCallback.IRequestProcess {

    companion object{
        private const val TAG = "DefaultRequestProcess"
    }

    override fun requestContinue() {
        LogUtil.d(TAG, "requestContinue")
        val request = chain.getRequest()
        request.dispatchRequestStep { callback ->
            callback.onRequestResume(REASON_REQUEST_CALLBACK)
        }
        chain.process(request)
    }

    override fun requestTermination() {
        LogUtil.d(TAG, "requestTermination")
        chain.process(chain.getRequest(), finish = true)
    }
}