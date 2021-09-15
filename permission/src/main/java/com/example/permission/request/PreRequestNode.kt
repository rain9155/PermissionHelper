package com.example.permission.request

import com.example.permission.base.*
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IRequestStepCallback
import com.example.permission.base.REASON_REQUEST_CALLBACK
import com.example.permission.base.Request
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil

/**
 * 权限请求的前置处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class PreRequestNode : INode, IRequestStepCallback.Impl() {

    companion object{
        private const val TAG = "PreRequestNode"
    }

    private var pauseReason = -1
    private var pauseRequest: Request? = null

    private val proxyFragmentUpdateCallback = object : IProxyFragmentUpdateCallback {
        override fun onProxyFragmentUpdate(proxyFragment: IProxyFragment) {
            LogUtil.d(TAG, "onProxyFragmentUpdate: pauseReason = $pauseReason")
            pauseRequest?.also { request ->
                if(pauseReason == REASON_REQUEST_CALLBACK){
                    callOnRequestCallback(request)
                }
            }
        }
    }

    override fun onRequestStart(request: Request) {
        super.onRequestStart(request)
        if(request.reCallbackAfterConfigurationChanged){
            request.getProxyFragment().obtainFragmentUpdateCallbackManager().add(proxyFragmentUpdateCallback)
        }
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
        if(request.reCallbackAfterConfigurationChanged){
            request.getProxyFragment().obtainFragmentUpdateCallbackManager().remove(proxyFragmentUpdateCallback)
        }
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            if(request.isRestart){
                return@dispatchRequestStep
            }
            callback.onRequestStart(request)
        }

        val result = PermissionUtil.checkPermissions(request.getActivity(), request.getClonedRequestPermissions())
        request.requestPermissions = result.first
        request.grantedPermissions.addAll(result.second)

        if(!callOnRequestCallback(request)) {
            chain.process(request)
        }
    }

    private fun callOnRequestCallback(request: Request): Boolean {
        if(request.requestCallback != null && request.requestPermissions.isNotEmpty()){
            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPause(request, REASON_REQUEST_CALLBACK)
            }
            request.requestCallback!!.onRequest(
                DefaultRequestProcess(request.linkedChain!!),
                request.getClonedRequestPermissions()
            )
            return true
        }
        return false
    }

}