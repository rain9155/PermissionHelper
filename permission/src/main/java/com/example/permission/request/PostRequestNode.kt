package com.example.permission.request

import com.example.permission.base.*
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IRequestStepCallback
import com.example.permission.base.REASON_REJECTED_CALLBACK
import com.example.permission.base.REASON_REJECTED_FOREVER_CALLBACK
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import java.lang.Exception


/**
 * 权限请求的后置处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class PostRequestNode : INode, IRequestStepCallback.Impl() {

    companion object{
        private const val TAG = "PostRequestNode"
    }

    private var pauseReason = -1
    private var pauseRequest: Request? = null

    private val proxyFragmentUpdateCallback = object : IProxyFragmentUpdateCallback {
        override fun onProxyFragmentUpdate(proxyFragment: IProxyFragment) {
            LogUtil.d(TAG, "onProxyFragmentUpdate: pauseReason = $pauseReason")
            pauseRequest?.also { request ->
                if(pauseReason == REASON_REJECTED_CALLBACK) {
                    if(request.reCallbackAfterConfigurationChanged) {
                        callOnRejectedCallback(request)
                    }else {
                        request.isInterrupt = true
                        request.linkedChain?.process(request, finish = true)
                    }
                }else if(pauseReason == REASON_REJECTED_FOREVER_CALLBACK) {
                    if(request.reCallbackAfterConfigurationChanged) {
                        callOnRejectedForeverCallback(request)
                    }else {
                        request.isInterrupt = true
                        request.linkedChain?.process(request, finish = true)
                    }
                }
            }
        }
    }

    override fun onRequestStart(request: Request) {
        super.onRequestStart(request)
        request.getProxyFragment().obtainFragmentUpdateCallbackManager().add(proxyFragmentUpdateCallback)
    }

    override fun onRequestPause(request: Request, reason: Int) {
        super.onRequestPause(request, reason)
        pauseReason = reason
        pauseRequest = request
    }

    override fun onRequestResume(request: Request, reason: Int) {
        super.onRequestResume(request, reason)
        pauseReason = -1
        pauseRequest = null
    }

    override fun onRequestFinish(request: Request) {
        super.onRequestFinish(request)
        request.getProxyFragment().obtainFragmentUpdateCallbackManager().remove(proxyFragmentUpdateCallback)
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        if(request.requestPermissions.isNotEmpty()){
            request.rejectedPermissions.addAll(request.requestPermissions)
            request.requestPermissions.clear()
        }

        val rejectedPermissions = request.getClonedRejectedPermissions()
        rejectedPermissions.forEach { permission ->
            if(!PermissionUtil.checkShouldShowRationale(request.getActivity(), permission)){
                request.rejectedPermissions.remove(permission)
                request.rejectedForeverPermissions.add(permission)
            }
        }

        if(!callOnRejectedCallback(request) && !callOnRejectedForeverCallback(request)) {
            chain.process(request)
        }
    }

    private fun callOnRejectedCallback(request: Request): Boolean {
        if(request.rejectedCallback != null && request.rejectedPermissions.isNotEmpty()){
            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPause(request, REASON_REJECTED_CALLBACK)
            }
            try {
                LogUtil.d(TAG, "callOnRejectedCallback")
                request.rejectedCallback!!.onRejected(
                    DefaultRejectedProcess(request.linkedChain!!),
                    request.getClonedRejectedPermissions()
                )
            }catch (e: Exception) {
                LogUtil.e(TAG, "callOnRejectedCallback: e = $e")
                request.isInterrupt = true
                request.linkedChain?.process(request, finish = true)
                throw e
            }
            return true
        }
        return false
    }

    private fun callOnRejectedForeverCallback(request: Request): Boolean {
        if (request.rejectedForeverCallback != null && request.rejectedForeverPermissions.isNotEmpty()) {
            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPause(request, REASON_REJECTED_FOREVER_CALLBACK)
            }
            try {
                LogUtil.d(TAG, "callOnRejectedForeverCallback")
                request.rejectedForeverCallback!!.onRejectedForever(
                    DefaultRejectedForeverProcess(request.linkedChain!!),
                    request.getClonedRejectedForeverPermissions()
                )
            }catch (e: Exception) {
                LogUtil.e(TAG, "callOnRejectedForeverCallback: e = $e")
                request.isInterrupt = true
                request.linkedChain?.process(request, finish = true)
                throw e
            }
            return true
        }
        return false
    }

}