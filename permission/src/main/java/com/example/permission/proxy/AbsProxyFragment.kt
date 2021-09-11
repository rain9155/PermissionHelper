package com.example.permission.proxy

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.example.permission.base.*
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.IProxyFragment
import com.example.permission.base.IProxyFragmentUpdateCallback
import com.example.permission.utils.LogUtil
import com.example.permission.utils.toStrings
import java.lang.reflect.Method

/**
 * 代理fragment的公共实现
 * Created by 陈健宇 at 2021/8/30
 */
internal abstract class AbsProxyFragment<T : ProxyFragmentViewModel> : Fragment(), IProxyFragment {

    companion object{
        private const val TAG = "AbsProxyFragment"
    }

    protected lateinit var host: FragmentActivity
    protected lateinit var viewModel: T
    protected lateinit var waitForCheckPermissions: SparseArray<Array<String>>
    protected lateinit var waitForCheckSpecialPermissions: SparseArray<SpecialArray>
    protected lateinit var requestNormalPermissionsResultLiveData: MutableLiveData<PermissionsResult>
    protected lateinit var requestSpecialPermissionsResultLiveData: MutableLiveData<PermissionsResult>
    protected lateinit var checkPermissionsResultLiveData: MutableLiveData<PermissionsResult>

    protected lateinit var permissionResultCallbacks: SparseArray<IPermissionResultsCallback>
    protected lateinit var proxyFragmentUpdateCallbacks: ArrayList<IProxyFragmentUpdateCallback>

    private var pendingAddProxyFragmentUpdateCallbacks = ArrayList<IProxyFragmentUpdateCallback>()
    private var pendingRemoveProxyFragmentUpdateCallbacks = ArrayList<IProxyFragmentUpdateCallback>()

    private val fragmentUpdateCallbackManager = object : IProxyFragment.FragmentUpdateCallbackManager{

        override fun add(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::proxyFragmentUpdateCallbacks.isInitialized){
                proxyFragmentUpdateCallbacks.add(fragmentUpdateCallback)
            }else{
                pendingAddProxyFragmentUpdateCallbacks.add(fragmentUpdateCallback)
            }
        }

        override fun remove(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::proxyFragmentUpdateCallbacks.isInitialized){
                proxyFragmentUpdateCallbacks.remove(fragmentUpdateCallback)
            }else{
                pendingRemoveProxyFragmentUpdateCallbacks.add(fragmentUpdateCallback)
            }
        }

        override fun contain(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::proxyFragmentUpdateCallbacks.isInitialized){
                proxyFragmentUpdateCallbacks.contains(fragmentUpdateCallback)
            }else{
                !pendingRemoveProxyFragmentUpdateCallbacks.contains(fragmentUpdateCallback)
                        && pendingAddProxyFragmentUpdateCallbacks.contains(fragmentUpdateCallback)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(TAG, "onCreate, savedState = $savedInstanceState")

        host = requestActivity()
        viewModel = createViewModel().apply {
            this@AbsProxyFragment.waitForCheckPermissions = waitForCheckPermissions
            this@AbsProxyFragment.waitForCheckSpecialPermissions = waitForCheckSpecialPermissions
            this@AbsProxyFragment.requestNormalPermissionsResultLiveData = requestNormalPermissionsResultLiveData
            this@AbsProxyFragment.requestSpecialPermissionsResultLiveData = requestSpecialPermissionsResultLiveData
            this@AbsProxyFragment.checkPermissionsResultLiveData = checkPermissionsResultLiveData
            this@AbsProxyFragment.permissionResultCallbacks = permissionResultCallbacks
            this@AbsProxyFragment.proxyFragmentUpdateCallbacks = proxyFragmentUpdateCallbacks
        }
        if(pendingAddProxyFragmentUpdateCallbacks.isNotEmpty()){
            proxyFragmentUpdateCallbacks.addAll(pendingAddProxyFragmentUpdateCallbacks)
            pendingAddProxyFragmentUpdateCallbacks.clear()
        }
        if(pendingRemoveProxyFragmentUpdateCallbacks.isNotEmpty()){
            proxyFragmentUpdateCallbacks.removeAll(pendingRemoveProxyFragmentUpdateCallbacks)
            pendingRemoveProxyFragmentUpdateCallbacks.clear()
        }
        proxyFragmentUpdateCallbacks.forEach { callback -> callback.onProxyFragmentUpdate(this) }
    }

    override fun onStart() {
        super.onStart()
        LogUtil.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.d(TAG, "onStop")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        LogUtil.d(TAG, "onSaveInstanceState")
        outState.putString("testKey", "testValue")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(TAG, "onDestroy")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.d(TAG, "onRequestPermissionsResult: requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LogUtil.d(TAG, "onActivityResult: requestCode = $requestCode, data = $data")
    }

    override fun requestFragmentManager(): FragmentManager {
        var getParentFragmentManagerMethod: Method? = null
        try {
           getParentFragmentManagerMethod =  this::class.java.getMethod("getParentFragmentManager")
        }catch (e: Exception){
            LogUtil.e(TAG, "requestFragmentManager: e = $e")
        }
        if(getParentFragmentManagerMethod != null){
            return getParentFragmentManagerMethod.invoke(this) as FragmentManager
        }
        return this::class.java.getMethod("getFragmentManager").invoke(this)
                as? FragmentManager
                ?: throw IllegalStateException("Fragment $this not associated with a fragment manager")
    }

    override fun obtainFragmentUpdateCallbackManager() = fragmentUpdateCallbackManager

    override fun obtainLifecycle() = lifecycle

    override fun requestActivity() = requireActivity()

    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) = requestNormalPermissions(permissions.toTypedArray(), callback)

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback) = requestSpecialPermissions(permissions.toTypedArray(), callback)

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) = startSettingsForCheckResults(permissions.toTypedArray(), callback)

    @CallSuper
    protected open fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestNormalPermissions: permissions = ${permissions.toStrings()}")
    }

    @CallSuper
    protected open fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = ${permissions.toStrings()}")
    }

    @CallSuper
    protected open fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = ${permissions.toStrings()}")
    }

    @CallSuper
    protected open fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray){
        if(permissionResultCallbacks[requestCode] != null){
            LogUtil.d(TAG, "handlePermissionsResult: requestCode = $requestCode, permissions = ${permissions.toStrings()}, grantResults = ${grantResults.toStrings()}")
        }else{
            LogUtil.d(TAG, "handlePermissionsResult: permission result callback is empty, requestCode = $requestCode, permissions = ${permissions.toStrings()}, grantResults = ${grantResults.toStrings()}")
        }
    }

    protected abstract fun createViewModel(): T

    protected abstract fun generateRequestCode(): Int

}