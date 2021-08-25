package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.example.permission.base.AbsProxyFragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.*
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SpecialUtil
import com.example.permission.proxy.ProxyFragmentV2ViewModel.LauncherAndCallback

/**
 * 申请权限的代理Fragment, 使用新的Activity Result API实现：https://developer.android.com/training/basics/intents/result#separate
 * Created by 陈健宇 at 2021/8/18
 */
internal class ProxyFragmentV2 : AbsProxyFragment(){

    companion object {
        private const val TAG = "ProxyFragmentV2"
        private const val INITIAL_REQUEST_CODE = 0x0000100

        fun newInstance(): ProxyFragmentV2 {
            return ProxyFragmentV2()
        }
    }

    private val host = requestActivity()
    private val registry = requireActivity().activityResultRegistry
    private val viewModel: ProxyFragmentV2ViewModel = getViewModel(this, ProxyFragmentV2ViewModel::class.java)
    private val requestPermissionsLauncherAndCallbacks = viewModel.requestPermissionsLauncherAndCallbacks
    private val startActivityLauncherAndCallbacks = viewModel.startActivityLauncherAndCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(requestPermissionsLauncherAndCallbacks.isNotEmpty()){
            requestPermissionsLauncherAndCallbacks.forEach { launcherAndCallback ->
                launcherAndCallback.launcher = registerForResultsWithNotLifecycle(
                    launcherAndCallback.key,
                    ActivityResultContracts.RequestMultiplePermissions(),
                    launcherAndCallback.callback
                )
            }
        }
        if(startActivityLauncherAndCallbacks.isNotEmpty()){
            startActivityLauncherAndCallbacks.forEach { launcherAndCallback ->
                launcherAndCallback.launcher = registerForResultsWithNotLifecycle(
                    launcherAndCallback.key,
                    ActivityResultContracts.StartActivityForResult(),
                    launcherAndCallback.callback
                )
            }
        }
    }

    override fun requestActivity(): Activity {
        return requireActivity()
    }

    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestNormalPermissions(permissions.toTypedArray(), callback)
    }

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestSpecialPermissions(permissions.toTypedArray(), callback)
    }

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) {
        startSettingsForCheckResults(permissions.toTypedArray(), callback)
    }

    override fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestNormalPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val resultCallback = {results: Map<String, Boolean> ->
            requestPermissionsLauncherAndCallbacks[requestCode].launcher.unregister()
            requestPermissionsLauncherAndCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(results.size)
            results.keys.forEachIndexed{ index, name ->
                permissionResults[index] = PermissionResult(name, results[name]!!)
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onRequestPermissionsResult: permissionResults = $permissionResults")
        }
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.RequestMultiplePermissions(), resultCallback)
        requestPermissionsLauncherAndCallbacks.put(requestCode, LauncherAndCallback(requestCode, launcher, resultCallback))
        launcher.launch(permissions)
    }

    override fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val specialPermissions = SpecialArray(permissions)
        val requestSpecialPermission = {launcher: ActivityResultLauncher<Intent>, permission: String ->
            launcher.launch(SpecialUtil.getIntent(host, permission))
        }
        val resultCallback = { result: ActivityResult ->
            specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
            val launcher = startActivityLauncherAndCallbacks[requestCode].launcher
            if(specialPermissions.hasNext()){
                requestSpecialPermission.invoke(launcher, specialPermissions.nextPermission())
            }else{
                launcher.unregister()
                startActivityLauncherAndCallbacks.remove(requestCode)
                val permissionResults = ArrayList<PermissionResult>(specialPermissions.size())
                specialPermissions.getGrantResults().forEachIndexed { index, granted ->
                    permissionResults[index] = PermissionResult(specialPermissions.get(index), granted, special = true)
                }
                callback.onPermissionResults(permissionResults)
                LogUtil.d(TAG, "onStartActivityForResults: permissionResults = $permissionResults")
            }
        }
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        startActivityLauncherAndCallbacks.put(requestCode, LauncherAndCallback(requestCode, launcher, resultCallback))
        requestSpecialPermission.invoke(launcher, specialPermissions.nextPermission())
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val resultCallback = {result: ActivityResult ->
            startActivityLauncherAndCallbacks[requestCode].launcher.unregister()
            startActivityLauncherAndCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            permissions.forEachIndexed { index, permission ->
                val permissionResult = if(PermissionUtil.isSpecialPermission(permission)){
                    PermissionResult(permission, PermissionUtil.checkSpecialPermission(host, permission), special = true)
                }else{
                    PermissionResult(permission, PermissionUtil.checkNormalPermission(host, permission))
                }
                permissionResults[index] = permissionResult
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onStartActivityForResults: permissionResults = $permissionResults")
        }
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        startActivityLauncherAndCallbacks.put(requestCode, LauncherAndCallback(requestCode, launcher, resultCallback))
        launcher.launch(SettingsUtil.getIntent(host))
    }

    override fun generateRequestCode(): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = INITIAL_REQUEST_CODE)
        } while (requestPermissionsLauncherAndCallbacks.indexOfKey(requestCode) >= 0
                || startActivityLauncherAndCallbacks.indexOfKey(requestCode) >= 0)
        return requestCode
    }

    private fun <I, O> registerForResultsWithNotLifecycle(key: Int, contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
        return registry.register(key.toString(), contract, callback)
    }
}