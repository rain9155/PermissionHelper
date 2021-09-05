package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SpecialUtil

/**
 * 权限请求，请求特殊权限
 * Created by 陈健宇 at 2021/8/16
 */
internal class RequestSpecialNode : INode {

    companion object{
        private const val TAG = "RequestSpecialNode"
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        LogUtil.d(TAG, "handle: request = $request")

        val specialPermissions = ArrayList<String>()
        request.requestPermissions.forEach {permission ->
            if(SpecialUtil.isSpecialPermission(permission)){
                specialPermissions.add(permission)
            }
        }

        request.dispatchRequestStep { callback ->
            callback.onRequestPermissions(specialPermissions)
        }

        request.getProxyFragment().requestSpecialPermissions(specialPermissions, object : IPermissionResultsCallback {
            override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                permissionResults.forEach {result ->
                    if(result.granted){
                        request.grantedPermissions.add(result.name)
                    }else{
                        request.rejectedPermissions.add(result.name)
                    }
                    request.requestPermissions.remove(result.name)
                }
                chain.process(request)
            }
        })
    }
}