package com.example.permission.proxy

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.IProxyFragment
import com.example.permission.base.IProxyFragmentUpdateCallback
import com.example.permission.proxy.ProxyFragmentViewModel.PermissionsResult
import com.example.permission.utils.LogUtil
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
    protected lateinit var permissionResultCallbacks: SparseArray<IPermissionResultsCallback>
    protected lateinit var waitForCheckPermissions: SparseArray<Array<String>>
    protected lateinit var waitForCheckSpecialPermissions: SparseArray<SpecialArray>
    protected lateinit var requestNormalPermissionsResultLiveData: MutableLiveData<PermissionsResult>
    protected lateinit var requestSpecialPermissionsResultLiveData: MutableLiveData<PermissionsResult>
    protected lateinit var checkPermissionsResultLiveData: MutableLiveData<PermissionsResult>

    private val pendingProxyFragmentUpdateCallbacks = ArrayList<IProxyFragmentUpdateCallback>()

    private val fragmentUpdateCallbackManager = object : IProxyFragment.FragmentUpdateCallbackManager{

        override fun add(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::viewModel.isInitialized){
                viewModel.proxyFragmentUpdateCallbacks.add(fragmentUpdateCallback)
            }else{
                pendingProxyFragmentUpdateCallbacks.add(fragmentUpdateCallback)
            }
        }

        override fun remove(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::viewModel.isInitialized){
                viewModel.proxyFragmentUpdateCallbacks.remove(fragmentUpdateCallback)
            }else{
                pendingProxyFragmentUpdateCallbacks.remove(fragmentUpdateCallback)
            }
        }

        override fun contain(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean {
            return if(::viewModel.isInitialized){
                viewModel.proxyFragmentUpdateCallbacks.contains(fragmentUpdateCallback)
            }else{
                pendingProxyFragmentUpdateCallbacks.contains(fragmentUpdateCallback)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach: isAdded = $isAdded")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(TAG, "onCreate")
        host = requestActivity()
        viewModel = createViewModel().apply {
            this@AbsProxyFragment.permissionResultCallbacks = permissionResultCallbacks
            this@AbsProxyFragment.waitForCheckPermissions = waitForCheckPermissions
            this@AbsProxyFragment.waitForCheckSpecialPermissions = waitForCheckSpecialPermissions
            this@AbsProxyFragment.requestNormalPermissionsResultLiveData = requestNormalPermissionsResultLiveData
            this@AbsProxyFragment.requestSpecialPermissionsResultLiveData = requestSpecialPermissionsResultLiveData
            this@AbsProxyFragment.checkPermissionsResultLiveData = checkPermissionsResultLiveData
        }
        if(pendingProxyFragmentUpdateCallbacks.isNotEmpty()){
            viewModel.proxyFragmentUpdateCallbacks.addAll(pendingProxyFragmentUpdateCallbacks)
            pendingProxyFragmentUpdateCallbacks.clear()
        }
        viewModel.proxyFragmentUpdateCallbacks.forEach{ callback -> callback.onProxyFragmentUpdate(this)}
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
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(TAG, "onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach: isAdded = $isAdded, isDetached = $isDetached")
    }

    override fun requestFragmentManager(): FragmentManager {
        var getParentFragmentManagerMethod: Method? = null
        try {
           getParentFragmentManagerMethod =  this::class.java.getMethod("getParentFragmentManager")
        }catch (e: Exception){
            e.printStackTrace()
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

    protected abstract fun createViewModel(): T

    protected abstract fun generateRequestCode(): Int

    protected abstract fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray)

}