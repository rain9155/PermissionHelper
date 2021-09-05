package com.example.permission.base

import android.app.Activity
import android.util.Log
import com.example.permission.IRejectedCallback
import com.example.permission.IRejectedForeverCallback
import com.example.permission.IRequestCallback
import com.example.permission.IResultCallback
import com.example.permission.proxy.ProxyFragmentProvider
import com.example.permission.utils.LogUtil

/**
 * 权限请求过程使用责任链模式，这里定义了责任链的的输入、连接、节点
 * Created by 陈健宇 at 2021/8/15
 */

/**
 * 权限请求过程中一些主要步骤的回调
 */
internal interface IRequestStepCallback{

    fun onRequestStart()

    fun onRequestPause(reason: Int)

    fun onRequestResume(reason: Int)

    fun onRequestPermissions(permissions: List<String>)

    fun onRequestFinish()

    open class Impl : IRequestStepCallback{

        override fun onRequestStart() {

        }

        override fun onRequestPause(reason: Int) {
        }

        override fun onRequestResume(reason: Int) {
        }

        override fun onRequestPermissions(permissions: List<String>) {

        }

        override fun onRequestFinish() {

        }
    }
}

/**
 * 输入
 */
internal data class Request(
    private val proxyFragmentProvider: ProxyFragmentProvider,
    var requestCallback: IRequestCallback?,
    var rejectedCallback: IRejectedCallback?,
    var rejectedForeverCallback: IRejectedForeverCallback?,
    var resultCallback: IResultCallback?,
    var requestPermissions: MutableList<String>,
    val grantedPermissions: MutableList<String> = ArrayList(),
    val rejectedPermissions: MutableList<String> = ArrayList(),
    val rejectedForeverPermissions: MutableList<String> = ArrayList(),
    var isRestart: Boolean = false
) : IRequestStepCallback.Impl(){

    companion object{
        private const val TAG = "Request"
    }

    private val requestStepCallbacks = ArrayList<IRequestStepCallback>()

    init {
        requestStepCallbacks.run {
            add(this@Request)
            add(proxyFragmentProvider)
        }
    }

    override fun onRequestStart() {
        super.onRequestStart()
        LogUtil.d(TAG, "onRequestStart")
    }

    override fun onRequestPause(reason: Int) {
        super.onRequestPause(reason)
        LogUtil.d(TAG, "onRequestPause: reason = $reason")
    }

    override fun onRequestResume(reason: Int) {
        super.onRequestResume(reason)
        LogUtil.d(TAG, "onRequestResume: reason = $reason")
    }

    override fun onRequestPermissions(permissions: List<String>) {
        super.onRequestPermissions(permissions)
        LogUtil.d(TAG, "onRequestPermissions: permissions = $permissions")
    }

    override fun onRequestFinish() {
        super.onRequestFinish()
        LogUtil.d(TAG, "onRequestFinish")
    }

    fun getProxyFragment(): IProxyFragment{
        return proxyFragmentProvider.get()
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

    fun dispatchRequestStep(callback: (IRequestStepCallback) -> Unit){
        requestStepCallbacks.forEach { callback.invoke(it) }
    }

    fun clearRequestStepCallback(){
        requestStepCallbacks.clear()
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