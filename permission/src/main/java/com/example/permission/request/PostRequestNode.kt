package com.example.permission.request

import android.app.Activity
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.REASON_REJECTED_CALLBACK
import com.example.permission.base.REASON_REJECTED_FOREVER_CALLBACK
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import java.util.LinkedHashSet

/**
 * 权限请求的后置处理
 * Created by 陈健宇 at 2021/8/16
 */
internal class PostRequestNode : INode {

    companion object{
        private const val TAG = "PostRequestNode"
    }

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        if(request.requestPermissions.isNotEmpty()){
            request.rejectedPermissions.addAll(request.requestPermissions)
            request.requestPermissions.clear()
        }

        val rejectedPermissions = request.getClonedRejectedPermissions()
        rejectedPermissions.forEach { permission ->
            if(!PermissionUtil.checkShouldShowRationale(request.getActivity(), permission)){
                request.rejectedPermissions.remove(permission)
                request.rejectedForeverPermissions.add(permission)
            }
        }

        LogUtil.d(TAG, "handle: request = $request")

        if(request.rejectedCallback != null && request.rejectedPermissions.isNotEmpty()){
            request.dispatchRequestStep { callback ->
                callback.onRequestPause(REASON_REJECTED_CALLBACK)
            }
            request.rejectedCallback!!.onRejected(
                DefaultRejectedProcess(chain),
                request.getClonedRejectedPermissions()
            )
            request.rejectedCallback = null
            return
        }

        if(request.rejectedForeverCallback != null && request.rejectedForeverPermissions.isNotEmpty()){
            request.dispatchRequestStep { callback ->
                callback.onRequestPause(REASON_REJECTED_FOREVER_CALLBACK)
            }
            request.rejectedForeverCallback!!.onRejectedForever(
                DefaultRejectedForeverProcess(chain),
                request.getClonedRejectedForeverPermissions()
            )
            request.rejectedForeverCallback = null
            return
        }

        chain.process(request)
    }

}