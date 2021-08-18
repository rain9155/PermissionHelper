package com.example.permission.request

import android.app.Activity
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import java.util.LinkedHashSet

/**
 * 权限请求的后置处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class PostRequestNode : INode {

    companion object{
        private const val TAG = "RequestSpecialNode"
    }

    //保存上一次request时已经永远拒绝过的权限
    private val preRejectedForeverPermissions = ArrayList<String>()

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        LogUtil.d(TAG, "pre handle: request = $request")

        var hasCallback = false

        if(request.rejectedCallback != null){
            val shouldRationalePermissions = ArrayList<String>()
            request.requestPermissions.forEach {permission ->
                if(isShouldRationale(request.activity, permission)){
                    shouldRationalePermissions.add(permission)
                }else{
                    preRejectedForeverPermissions.add(permission)
                }
            }
            if(shouldRationalePermissions.isNotEmpty()){
                request.requestPermissions = shouldRationalePermissions
                request.rejectedCallback!!.onRejected(
                    DefaultRejectedProcess(chain),
                    shouldRationalePermissions
                )
                request.rejectedCallback = null
                hasCallback = true
            }else{
                preRejectedForeverPermissions.clear()
            }
        }

        if(!hasCallback && request.rejectedForeverCallback != null){
            val rejectedForeverPermissions = ArrayList<String>()
            request.requestPermissions.forEach {permission ->
                if(isShouldRationale(request.activity, permission)){
                    request.rejectedPermissions.add(permission)
                }else{
                    rejectedForeverPermissions.add(permission)
                }
            }
            if(preRejectedForeverPermissions.isNotEmpty()){
                rejectedForeverPermissions.addAll(preRejectedForeverPermissions)
                preRejectedForeverPermissions.clear()
            }
            if(rejectedForeverPermissions.isNotEmpty()){
                request.requestPermissions = rejectedForeverPermissions
                request.rejectedForeverCallback!!.onRejectedForever(
                    DefaultRejectedForeverProcess(chain),
                    rejectedForeverPermissions
                )
                request.rejectedForeverCallback = null
                hasCallback = true
            }
        }

        if(!hasCallback){
            chain.process(request)
        }

        LogUtil.d(TAG, "post handle: request = $request")
    }

    private fun isShouldRationale(activity: Activity, permission: String): Boolean{
        return PermissionUtil.isSpecialPermission(permission) || PermissionUtil.checkShouldShowRationale(activity, permission)
    }
}