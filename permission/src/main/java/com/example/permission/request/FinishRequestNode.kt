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

        LogUtil.d(TAG, "pre handle: request = $request")

        request.rejectedPermissions.addAll(request.requestPermissions)
        if(request.resultCallback != null){
            request.resultCallback!!.onResult(
                request.rejectedPermissions.isEmpty(),
                request.grantedPermissions,
                request.rejectedPermissions
            )
            request.resultCallback = null
        }

        LogUtil.d(TAG, "post handle: request = $request")
    }
}