package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.utils.LogUtil

/**
 * 权限请求结果回调处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class FinishRequestNode : INode {

    companion object{
        private const val TAG = "FinishRequestNode"
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            callback.onRequestFinish(request)
        }
        request.getRequestStepCallbackManager().clear()

        DefaultRequestManager.instance.finishRequest(request.requestKey)

        if(!request.isInterrupt && request.requestPermissions.isNotEmpty()){
            request.rejectedPermissions.addAll(request.requestPermissions)
            request.requestPermissions.clear()
        }

        LogUtil.d(TAG, "handle: request = $request")

        if(!request.isInterrupt && request.resultCallback != null){
            val grantedPermissions = request.getClonedGrantedPermissions()
            val rejectedPermissions = ArrayList<String>(request.rejectedPermissions.size + request.rejectedForeverPermissions.size).apply {
                addAll(request.rejectedPermissions)
                addAll(request.rejectedForeverPermissions)
            }
            request.resultCallback!!.onResult(
                rejectedPermissions.isEmpty(),
                grantedPermissions,
                rejectedPermissions
            )
            request.resultCallback = null
        }
    }
}