package com.example.permission.request

import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil

/**
 * 权限请求，请求除特殊权限以外的权限
 * Created by 陈健宇 at 2021/8/15
 */
internal class RequestNormalNode : INode {

    companion object{
        private const val TAG = "RequestNormalNode"
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        LogUtil.d(TAG, "pre handle: request = $request")

        val normalPermissions = ArrayList<String>()
        request.requestPermissions.forEach {permission ->
            if(!PermissionUtil.isSpecialPermission(permission)){
                normalPermissions.add(permission)
            }
        }

        request.proxyFragment.requestNormalPermissions(normalPermissions, object : IPermissionResultsCallback {
            override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                permissionResults.forEach {result ->
                    if(result.granted && request.requestPermissions.remove(result.name)){
                        request.grantedPermissions.add(result.name)
                    }
                }
                chain.process(request)
            }
        })

        LogUtil.d(TAG, "post handle: request = $request")
    }

}