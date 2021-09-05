package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.REASON_REQUEST_CALLBACK
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil

/**
 * 权限请求的前置处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class PreRequestNode : INode {

    companion object{
        private const val TAG = "PreRequestNode"
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        request.dispatchRequestStep { callback ->
            if(request.isRestart){
                return@dispatchRequestStep
            }
            callback.onRequestStart()
        }

        val result = PermissionUtil.checkPermissions(request.getActivity(), request.getClonedRequestPermissions())
        request.requestPermissions = result.first
        request.grantedPermissions.addAll(result.second)

        LogUtil.d(TAG, "handle: request = $request")

        if(request.requestCallback != null && request.requestPermissions.isNotEmpty()){
            request.dispatchRequestStep { callback ->
                callback.onRequestPause(REASON_REQUEST_CALLBACK)
            }
            request.requestCallback!!.onRequest(
                DefaultRequestProcess(chain),
                request.getClonedRequestPermissions()
            )
            request.requestCallback = null
        }else{
            chain.process(request)
        }
    }

}