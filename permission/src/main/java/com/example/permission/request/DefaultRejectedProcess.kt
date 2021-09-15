package com.example.permission.request

import com.example.permission.IRejectedCallback
import com.example.permission.IRequestCallback
import com.example.permission.base.IChain
import com.example.permission.base.REASON_REJECTED_CALLBACK
import com.example.permission.utils.LogUtil

/**
 * [IRejectedCallback.IRejectedProcess]的默认实现
 * Created by 陈健宇 at 2021/8/18
 */
internal class DefaultRejectedProcess(private val chain: IChain) : IRejectedCallback.IRejectedProcess {

    companion object{
        private const val TAG = "DefaultRejectedProcess"
    }

    override fun requestAgain(permissions: List<String>) {
        LogUtil.d(TAG, "requestAgain: permissions = $permissions")
        val request = chain.getRequest()
        val rejectedPermissions = request.getClonedRejectedPermissions()
        rejectedPermissions.forEach { permission ->
            if(permissions.contains(permission)){
                request.rejectedPermissions.remove(permission)
            }
        }
        request.requestPermissions = permissions.toMutableList()
        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            callback.onRequestResume(request, REASON_REJECTED_CALLBACK)
        }
        request.rejectedCallback = null
        chain.process(request, restart = true)
    }

    override fun requestTermination() {
        LogUtil.d(TAG, "requestTermination")
        val request = chain.getRequest()
        request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
            callback.onRequestResume(request, REASON_REJECTED_CALLBACK)
        }
        request.rejectedCallback = null
        chain.process(chain.getRequest(), again = true)
    }

}