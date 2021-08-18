package com.example.permission.request

import com.example.permission.IRejectedForeverCallback
import com.example.permission.base.IChain
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil

/**
 * [IRejectedForeverCallback.IRejectedForeverProcess]的默认实现
 * Created by 陈健宇 at 2021/8/18
 */
internal class DefaultRejectedForeverProcess(private val chain: IChain) : IRejectedForeverCallback.IRejectedForeverProcess{

    companion object{
        private const val TAG = "DefaultRejectedForeverProcess"
    }

    override fun gotoSettings() {
        LogUtil.d(TAG, "gotoSettings")
        val request = chain.getRequest()
        request.proxyFragment.gotoSettingsForCheckResults(request.requestPermissions, object : IPermissionResultsCallback{
            override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                permissionResults.forEach {result ->
                    if(result.granted && request.requestPermissions.remove(result.name)){
                        request.grantedPermissions.add(result.name)
                    }
                }
                chain.process(request)
            }
        })
    }

    override fun requestTermination() {
        LogUtil.d(TAG, "requestTermination")
        chain.process(chain.getRequest(), finish = true)
    }
}