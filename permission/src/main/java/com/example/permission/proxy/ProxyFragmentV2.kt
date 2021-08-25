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
    private val requestNormalPermissionsLaunchedKeys = viewModel.requestNormalPermissionsLaunchedKeys
    private val requestSpecialPermissionsLaunchedKeys = viewModel.requestSpecialPermissionsLaunchedKeys
    private val checkPermissionsLaunchedKeys = viewModel.checkPermissionsLaunchedKeys
    private val permissionResultCallbacks = viewModel.permissionResultCallbacks
    private val waitForCheckPermissions = viewModel.waitForCheckPermissions
    private val waitForCheckSpecialPermissions = viewModel.waitForCheckSpecialPermissions
    private val launchedLaunchers = SparseArray<ActivityResultLauncher<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(requestSpecialPermissionsLaunchedKeys.isNotEmpty()){
            requestSpecialPermissionsLaunchedKeys.forEach { requestCode ->
            }
            requestSpecialPermissionsLaunchedKeys.forEach { requestCode ->
            }
            checkPermissionsLaunchedKeys.forEach { requestCode ->
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        launchedLaunchers.forEach { launcher ->
            launcher.unregister()
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
        val resultCallback = HandleRequestNormalPermissionsResultCallback(callback)
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.RequestMultiplePermissions(), resultCallback)
        resultCallback.launcher = launcher
        requestNormalPermissionsLaunchedKeys.add(requestCode)
        launchedLaunchers.put(requestCode, launcher)
        permissionResultCallbacks.put(requestCode, callback)
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
        val resultCallback = HandleRequestSpecialPermissionsResultCallback(
            host,
            specialPermissions,
            ::requestSpecialPermission,
            callback
        )
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        resultCallback.launcher = launcher
        requestSpecialPermissionsLaunchedKeys.add(requestCode)
        launchedLaunchers.put(requestCode, launcher)
        waitForCheckSpecialPermissions.put(requestCode, specialPermissions)
        permissionResultCallbacks.put(requestCode, callback)
        requestSpecialPermission(launcher, specialPermissions.nextPermission())
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val resultCallback = HandleCheckPermissionsResultCallback(
            host,
            permissions,
            callback
        )
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        resultCallback.launcher = launcher
        checkPermissionsLaunchedKeys.add(requestCode)
        launchedLaunchers.put(requestCode, launcher)
        waitForCheckPermissions.put(requestCode, permissions)
        permissionResultCallbacks.put(requestCode, callback)
        launcher.launch(SettingsUtil.getIntent(host))
    }

    override fun generateRequestCode(): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = INITIAL_REQUEST_CODE)
        } while (requestNormalPermissionsLaunchedKeys.contains(requestCode)
                || requestSpecialPermissionsLaunchedKeys.contains(requestCode)
                || checkPermissionsLaunchedKeys.contains(requestCode)
        )
        return requestCode
    }

    private fun <I, O> registerForResultsWithNotLifecycle(key: Int, contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
        return registry.register(key.toString(), contract, callback)
    }

    private fun requestSpecialPermission(launcher: ActivityResultLauncher<Intent>?, permission: String){
        launcher?.launch(SpecialUtil.getIntent(host, permission))
    }

    class HandleRequestNormalPermissionsResultCallback(
            private val callback: IPermissionResultsCallback
    ) : ActivityResultCallback<Map<String, Boolean>>{

        var launcher: ActivityResultLauncher<Array<String>>? = null

        override fun onActivityResult(result: Map<String, Boolean>) {
            launcher?.unregister()
            val permissionResults = ArrayList<PermissionResult>(result.size)
            result.keys.forEachIndexed{ index, name ->
                permissionResults[index] = PermissionResult(name, result[name]!!)
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onRequestNormalPermissionsResult: permissionResults = $permissionResults")
        }
    }

    class HandleRequestSpecialPermissionsResultCallback(
            private val host: Activity,
            private val specialPermissions: SpecialArray,
            private val requestSpecialPermission: (ActivityResultLauncher<Intent>?, String) -> Unit,
            private val callback: IPermissionResultsCallback
    ) : ActivityResultCallback<ActivityResult>{

        var launcher: ActivityResultLauncher<Intent>? = null

        override fun onActivityResult(result: ActivityResult?) {
            specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
            if(specialPermissions.hasNext()){
                requestSpecialPermission.invoke(launcher, specialPermissions.nextPermission())
            }else{
                launcher?.unregister()
                val permissionResults = ArrayList<PermissionResult>(specialPermissions.size())
                specialPermissions.getGrantResults().forEachIndexed { index, granted ->
                    permissionResults[index] = PermissionResult(specialPermissions.get(index), granted, special = true)
                }
                callback.onPermissionResults(permissionResults)
                LogUtil.d(TAG, "onRequestSpecialPermissionsResult: permissionResults = $permissionResults")
            }
        }
    }

    class HandleCheckPermissionsResultCallback(
        private val host: Activity,
        private val permissions: Array<String>,
        private val callback: IPermissionResultsCallback
    ) : ActivityResultCallback<ActivityResult>{

        var launcher: ActivityResultLauncher<Intent>? = null

        override fun onActivityResult(result: ActivityResult?) {
            launcher?.unregister()
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
            LogUtil.d(TAG, "onCheckPermissionsResult: permissionResults = $permissionResults")
        }
    }
}