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

    private var backgroundLocationPermission: String? = null

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        LogUtil.d(TAG, "handle: request = $request")

        val normalPermissions = ArrayList<String>()
        request.requestPermissions.forEach {permission ->
            if(!SpecialUtil.isSpecialPermission(permission)){
                normalPermissions.add(permission)
            }
        }

        if(TextUtils.isEmpty(backgroundLocationPermission)){
            backgroundLocationPermission = normalPermissions.find { permission ->
                Manifest.permission.ACCESS_BACKGROUND_LOCATION == permission
            }
        }
        normalPermissions.remove(backgroundLocationPermission)

        if(normalPermissions.isNotEmpty()){
            requestNormalPermissions(chain, request, normalPermissions)
        }else if(!TextUtils.isEmpty(backgroundLocationPermission)){
            requestNormalPermissions(chain, request, listOf(backgroundLocationPermission!!))
            backgroundLocationPermission = null
        }else{
            chain.process(request)
        }
    }

    fun requestNormalPermissions(chain: IChain, request: Request, normalPermissions: List<String>){

        request.dispatchRequestStep { callback ->
            callback.onRequestPermissions(normalPermissions)
        }

        request.getProxyFragment().requestNormalPermissions(normalPermissions, object : IPermissionResultsCallback {
            override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                permissionResults.forEach {result ->
                    if(result.granted){
                        request.grantedPermissions.add(result.name)
                    }else{
                        request.rejectedPermissions.add(result.name)
                    }
                    request.requestPermissions.remove(result.name)
                }
                if(!TextUtils.isEmpty(backgroundLocationPermission)){
                    chain.process(request, again = true)
                }else{
                    chain.process(request)
                }
            }
        })
    }

}