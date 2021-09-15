package com.example.permission.base

import android.app.Activity
import com.example.permission.IRejectedCallback
import com.example.permission.IRejectedForeverCallback
import com.example.permission.IRequestCallback
import com.example.permission.IResultCallback
import com.example.permission.proxy.ProxyFragmentProvider
import com.example.permission.utils.LogUtil
import com.example.permission.IProcess


/**
 * 权限请求过程使用责任链模式，这里定义了责任链的的输入、连接、节点
 * Created by 陈健宇 at 2021/8/15
 */

private const val TAG = "Request"

/**
 * 权限请求过程中一些主要步骤的回调
 */
internal interface IRequestStepCallback{

    fun onRequestStart(request: Request)

    fun onRequestPermissions(request: Request, permissions: List<String>)

    fun onRequestPause(request: Request, reason: Int)

    fun onRequestResume(request: Request, reason: Int)

    fun onRequestFinish(request: Request)

    open class Impl : IRequestStepCallback{

        override fun onRequestStart(request: Request) {}

        override fun onRequestPermissions(request: Request, permissions: List<String>) {}

        override fun onRequestPause(request: Request, reason: Int) {}

        override fun onRequestResume(request: Request, reason: Int) {}

        override fun onRequestFinish(request: Request) {}

    }
}

/**
 * 管理[IRequestStepCallback]的增删改查
 */
internal interface IRequestStepCallbackManager {

    fun dispatchRequestStep(callback: (IRequestStepCallback) -> Unit)

    fun add(callback: IRequestStepCallback): Boolean

    fun remove(callback: IRequestStepCallback): Boolean

    fun contain(callback: IRequestStepCallback): Boolean

    fun clear()
}

/**
 * 管理某个页面的[Request]
 */
internal interface IRequestManager {

    fun startRequest(request: Request)

    fun finishRequest(request: Request)

    fun clearRequests()

}

/**
 * 输入：
 * 1、在Request的过程中[requestPermissions]根据请求结果被划分到[grantedPermissions]、[rejectedPermissions]、[rejectedForeverPermissions]
 * 2、在一次Request的过程中[requestCallback]、[rejectedCallback]、[rejectedForeverCallback]在[IProcess]的相应方法回调时被消耗，即被置空
 * 3、每一次Request通过[requestKey]唯一标识，不会重复发起请求
 */
internal data class Request(
    val fragmentProvider: ProxyFragmentProvider,
    val reCallbackAfterConfigurationChanged: Boolean,
    var requestCallback: IRequestCallback?,
    var rejectedCallback: IRejectedCallback?,
    var rejectedForeverCallback: IRejectedForeverCallback?,
    var resultCallback: IResultCallback?,
    var requestPermissions: MutableList<String>,
    val grantedPermissions: MutableList<String> = ArrayList(),
    val rejectedPermissions: MutableList<String> = ArrayList(),
    val rejectedForeverPermissions: MutableList<String> = ArrayList(),
    var requestKey: String = "",
    var isRestart: Boolean = false,
    var linkedChain: IChain? = null
) : IRequestStepCallback.Impl() {

    private val requestStepCallbackManager = object : IRequestStepCallbackManager {

        private val requestStepCallbacks = ArrayList<IRequestStepCallback>()

        override fun dispatchRequestStep(callback: (IRequestStepCallback) -> Unit) {
            requestStepCallbacks.forEach { callback.invoke(it) }
        }

        override fun add(callback: IRequestStepCallback): Boolean {
            return requestStepCallbacks.add(callback)
        }

        override fun remove(callback: IRequestStepCallback): Boolean {
            return requestStepCallbacks.remove(callback)
        }

        override fun contain(callback: IRequestStepCallback): Boolean {
            return requestStepCallbacks.contains(callback)
        }

        override fun clear() {
            requestStepCallbacks.clear()
        }
    }

    override fun onRequestStart(request: Request) {
        super.onRequestStart(request)
        LogUtil.d(TAG, "onRequestStart: request = $request")
    }

    override fun onRequestPermissions(request: Request, permissions: List<String>) {
        super.onRequestPermissions(request, permissions)
        LogUtil.d(TAG, "onRequestPermissions: realRequestPermissions = $permissions")
    }

    override fun onRequestPause(request: Request, reason: Int) {
        super.onRequestPause(request, reason)
        LogUtil.d(TAG, "onRequestPause: reason = $reason")
    }

    override fun onRequestResume(request: Request, reason: Int) {
        super.onRequestResume(request, reason)
        LogUtil.d(TAG, "onRequestResume: reason = $reason")
    }

    override fun onRequestFinish(request: Request) {
        super.onRequestFinish(request)
        LogUtil.d(TAG, "onRequestFinish: request = $request")
    }

    fun getProxyFragment(): IProxyFragment{
        return fragmentProvider.get()
    }

    fun getActivity(): Activity{
        return getProxyFragment().requestActivity()
    }

    fun getClonedRequestPermissions(): List<String>{
        return ArrayList(requestPermissions)
    }

    fun getClonedGrantedPermissions(): List<String>{
        return ArrayList(grantedPermissions)
    }

    fun getClonedRejectedPermissions(): List<String>{
        return ArrayList(rejectedPermissions)
    }

    fun getClonedRejectedForeverPermissions(): List<String>{
        return ArrayList(rejectedForeverPermissions)
    }

    fun getRequestStepCallbackManager(): IRequestStepCallbackManager {
        return requestStepCallbackManager
    }
}

/**
 * 连接
 */
internal interface IChain {

    fun getRequest(): Request

    fun process(request: Request, finish: Boolean = false, restart: Boolean = false, again: Boolean = false)

}

/**
 * 节点
 */
internal interface INode {

    fun handle(chain: IChain)

}