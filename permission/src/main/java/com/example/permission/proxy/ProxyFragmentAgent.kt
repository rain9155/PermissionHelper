package com.example.permission.proxy

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.example.permission.base.*
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.IProxyFragment
import com.example.permission.base.IProxyFragmentUpdateCallback
import com.example.permission.base.PermissionResult
import com.example.permission.request.DefaultRequestManager
import com.example.permission.utils.LogUtil
import java.util.*

/**
 * 代理ProxyFragment，进行生命周期相关的控制
 * Created by 陈健宇 at 2021/8/31
 */
internal class ProxyFragmentAgent(private var activity: FragmentActivity, private var proxyFragment: IProxyFragment) : IProxyFragment, IProxyFragmentUpdateCallback{

    companion object{
        private const val TAG = "ProxyFragmentAgent"
    }

    private var proxyFragmentLifecycle = proxyFragment.obtainLifecycle()
    private val pendingRequestActions = LinkedList<Runnable>()
    private val pendingResultActions = LinkedList<Runnable>()

    private val proxyFragmentLifecycleObserver = object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event){
            LogUtil.d(TAG, "onStatChanged: source = $source, event = $event, agent = ${this@ProxyFragmentAgent}")
            if(event == Lifecycle.Event.ON_RESUME){
                if(pendingRequestActions.isNotEmpty()){
                    LogUtil.d(TAG, "executePendingRequestActions")
                    val requestActions = LinkedList(pendingRequestActions)
                    pendingRequestActions.clear()
                    executeActions(requestActions)
                }
                if(pendingResultActions.isNotEmpty()){
                    LogUtil.d(TAG, "executePendingResultActions")
                    val resultActions = LinkedList(pendingResultActions)
                    pendingResultActions.clear()
                    executeActions(resultActions)
                }
            }
        }

    }

    init {
        LogUtil.d(TAG, "initialize proxyFragmentAgent: agent = $this")
        observeProxyFragmentLifecycle()
    }

    override fun onProxyFragmentUpdate(proxyFragment: IProxyFragment) {
        LogUtil.d(TAG, "onProxyFragmentUpdate: proxyFragment = $proxyFragment, agent = ${this@ProxyFragmentAgent}")
        this@ProxyFragmentAgent.proxyFragment = proxyFragment
        activity = proxyFragment.requestActivity()
        proxyFragmentLifecycle = proxyFragment.obtainLifecycle()
        //代理fragment销毁重建重新订阅它的生命周期
        observeProxyFragmentLifecycle()
    }

    override fun isAttachActivity(): Boolean {
        return proxyFragment.isAttachActivity()
    }

    override fun requestActivity(): FragmentActivity {
        return if(isAttachActivity()) proxyFragment.requestActivity() else activity
    }

    override fun requestFragmentManager(): FragmentManager {
        return if(isAttachActivity()) proxyFragment.requestFragmentManager() else (object : FragmentManager(){})
    }

    override fun requestPageIdentify(): String {
        return if(isAttachActivity()) proxyFragment.requestPageIdentify() else activity::class.java.name
    }

    override fun obtainLifecycle(): Lifecycle {
        return proxyFragment.obtainLifecycle()
    }

    override fun obtainFragmentUpdateCallbackManager(): IFragmentUpdateCallbackManager {
        return proxyFragment.obtainFragmentUpdateCallbackManager()
    }

    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestOrPending(permissions, callback, proxyFragment::requestNormalPermissions)
    }

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestOrPending(permissions, callback, proxyFragment::requestSpecialPermissions)
    }

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestOrPending(permissions, callback, proxyFragment::gotoSettingsForCheckResults)
    }

    private fun requestOrPending(permissions: List<String>, callback: IPermissionResultsCallback, action: (List<String>, IPermissionResultsCallback) -> Unit){
        if(proxyFragmentLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)){
            LogUtil.d(TAG, "request: permissions = $permissions")
            action.invoke(permissions, PermissionResultsCallbackAgent(callback))
        }else{
            LogUtil.d(TAG, "pending request: permissions = $permissions")
            pendingRequestActions.offer(Runnable {
                action.invoke(permissions, PermissionResultsCallbackAgent(callback))
            })
        }
    }

    private fun resultOrPending(permissionResults: List<PermissionResult>, action: (List<PermissionResult>) -> Unit){
        if(proxyFragmentLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)){
            LogUtil.d(TAG, "result: permissionResults = $permissionResults")
            action.invoke(permissionResults)
        }else{
            LogUtil.d(TAG, "pending result: permissionResults = $permissionResults")
            pendingResultActions.offer(Runnable {
                action.invoke(permissionResults)
            })
        }
    }

    private fun executeActions(actions: LinkedList<Runnable>){
        LogUtil.d(TAG, "executeActions: actions size = ${actions.size}")
        while (actions.isNotEmpty()){
            actions.poll()?.run()
        }
    }

    private fun observeProxyFragmentLifecycle(){
        LogUtil.d(TAG, "observeProxyFragmentLifecycle")
        proxyFragmentLifecycle.addObserver(proxyFragmentLifecycleObserver)
    }

    inner class PermissionResultsCallbackAgent(private val callback: IPermissionResultsCallback) : IPermissionResultsCallback {

        override fun onPermissionResults(permissionResults: List<PermissionResult>) {
            resultOrPending(permissionResults, callback::onPermissionResults)
        }
        
    }
}