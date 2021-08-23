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

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        LogUtil.d(TAG, "pre handle: request = $request")

        if(request.requestPermissions.isNotEmpty()){
            request.rejectedPermissions.addAll(request.requestPermissions)
            request.requestPermissions.clear()
        }

        val rejectedPermissions = request.rejectedPermissions
        rejectedPermissions.forEach { permission ->
            if(!isShouldRationale(request.activity, permission)){
                request.rejectedForeverPermissions.add(permission)
                request.rejectedPermissions.remove(permission)
            }
        }

        if(request.rejectedCallback != null && request.rejectedPermissions.isNotEmpty()){
            request.rejectedCallback!!.onRejected(
                DefaultRejectedProcess(chain),
                request.rejectedPermissions
            )
            request.rejectedCallback = null
            return
        }

        if(request.rejectedForeverCallback != null && request.rejectedForeverPermissions.isNotEmpty()){
            request.rejectedForeverCallback!!.onRejectedForever(
                DefaultRejectedForeverProcess(chain),
                request.rejectedForeverPermissions
            )
            request.rejectedForeverCallback = null
            return
        }

        chain.process(request)

        LogUtil.d(TAG, "post handle: request = $request")
    }

    private fun isShouldRationale(activity: Activity, permission: String): Boolean{
        return PermissionUtil.isSpecialPermission(permission) || PermissionUtil.checkShouldShowRationale(activity, permission)
    }
}