package com.example.permission.request

import android.Manifest
import android.text.TextUtils
import com.example.permission.base.*
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.base.REASON_REQUEST_CALLBACK
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SpecialUtil

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
        if(!request.isRejectRequest) {
            val normalPermissions = request.requestPermissions.filter { permission ->
                !SpecialUtil.isSpecialPermission(permission)
            }

            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPermissions(request, normalPermissions)
            }

            request.getProxyFragment().requestNormalPermissions(normalPermissions, object : IPermissionResultsCallback {
                override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                    request.divisionRequestPermissionsByPermissionResults(permissionResults)
                    chain.process(request)
                }
            })
        }else {
            chain.process(request)
        }
    }

}