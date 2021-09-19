package com.example.permission.request

import com.example.permission.IRejectedForeverCallback
import com.example.permission.base.IChain
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.base.REASON_REJECTED_FOREVER_CALLBACK
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil

/**
 * [IRejectedForeverCallback.IRejectedForeverProcess]的默认实现
 * Created by 陈健宇 at 2021/8/18
 */
internal class DefaultRejectedForeverProcess(private val chain: IChain) : IRejectedForeverCallback.IRejectedForeverProcess {

    companion object{
        private const val TAG = "DefaultRejectedForeverProcess"
    }

    override fun gotoSettings() {
        LogUtil.d(TAG, "gotoSettings")
        val request = chain.getRequest()
        val permissions = ArrayList<String>(request.grantedPermissions.size + request.rejectedPermissions.size + request.rejectedForeverPermissions.size).apply {
            addAll(request.getClonedGrantedPermissions())
            addAll(request.getClonedRejectedPermissions())
            addAll(request.getClonedRejectedForeverPermissions())
        }
        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            callback.onRequestResume(request, REASON_REJECTED_FOREVER_CALLBACK)
        }
        request.rejectedForeverCallback = null
        request.getProxyFragment().gotoSettingsForCheckResults(permissions, object : IPermissionResultsCallback{
            override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                permissionResults.forEach {result ->
                    val granted = result.granted
                    val permission = result.name
                    if(request.rejectedForeverPermissions.contains(permission)){
                        if(granted){
                            request.rejectedForeverPermissions.remove(permission)
                            request.grantedPermissions.add(permission)
                        }
                    }else if(request.rejectedPermissions.contains(permission)){
                        if(granted){
                            request.rejectedPermissions.remove(permission)
                            request.grantedPermissions.add(permission)
                        }
                    }else{
                        if(!granted){
                            request.grantedPermissions.remove(permission)
                            if(result.shouldShowRationale) {
                                request.rejectedPermissions.add(permission)
                            }else {
                                request.rejectedForeverPermissions.add(permission)
                            }
                        }
                    }
                }
                chain.process(request)
            }
        })
    }

    override fun requestTermination() {
        LogUtil.d(TAG, "requestTermination")
        val request = chain.getRequest()
        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            callback.onRequestResume(request, REASON_REJECTED_FOREVER_CALLBACK)
        }
        request.rejectedForeverCallback = null
        chain.process(request, finish  = true)
    }
}