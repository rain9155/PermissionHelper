package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
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

        LogUtil.d(TAG, "pre handle: request = $request")

        val result = PermissionUtil.checkPermissions(request.activity, request.requestPermissions)
        request.requestPermissions = result.first
        request.grantedPermissions.addAll(result.second)

        if(request.requestCallback != null && request.requestPermissions.isNotEmpty()){
            request.requestCallback!!.onRequest(
                DefaultRequestProcess(chain),
                request.requestPermissions
            )
            request.requestCallback = null
        }else{
            chain.process(request)
        }

        LogUtil.d(TAG, "post handle: request = $request")
    }

}