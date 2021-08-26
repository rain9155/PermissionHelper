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
import com.example.permission.proxy.ProxyFragmentViewModel.*

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
    private val requestNormalPermissionsResultLiveData = viewModel.requestNormalPermissionsResultLiveData
    private val requestSpecialPermissionsResultLiveData = viewModel.requestSpecialPermissionsResultLiveData
    private val checkPermissionsResultLiveData = viewModel.checkPermissionsResultLiveData
    private val launchedLaunchers = SparseArray<ActivityResultLauncher<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNormalPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }
        requestSpecialPermissionsResultLiveData.observe(this) { result ->
            val requestCode = result.requestCode
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            if(specialPermissions.hasNext()){
                val launcher = launchedLaunchers[requestCode] as ActivityResultLauncher<Intent>
                requestSpecialPermission(launcher, specialPermissions.nextPermission())
            }else{
                waitForCheckSpecialPermissions.remove(requestCode)
                requestSpecialPermissionsLaunchedKeys.remove(requestCode)
                handlePermissionsResult(requestCode, result.permissions, result.grantResults)
            }
        }
        checkPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }

        requestSpecialPermissionsLaunchedKeys.forEach { requestCode -> registerForRequestNormalPermissionsResult(requestCode) }
        requestSpecialPermissionsLaunchedKeys.forEach { requestCode -> registerForRequestSpecialPermissionsResult(requestCode) }
        checkPermissionsLaunchedKeys.forEach { requestCode -> registerForCheckPermissionsResult(requestCode) }
    }

    override fun onDestroy() {
        super.onDestroy()
        launchedLaunchers.forEach { launcher -> launcher.unregister() }
        launchedLaunchers.clear()
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
        val launcher = registerForRequestNormalPermissionsResult(requestCode)
        requestNormalPermissionsLaunchedKeys.add(requestCode)
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
        val launcher = registerForRequestSpecialPermissionsResult(requestCode)
        requestSpecialPermissionsLaunchedKeys.add(requestCode)
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
        val launcher = registerForCheckPermissionsResult(requestCode)
        checkPermissionsLaunchedKeys.add(requestCode)
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

    override fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray) {
        val callback = permissionResultCallbacks[requestCode]
        if(callback != null){
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed{ index, grantResult ->
                permissionResults[index] = PermissionResult(permissions[index], grantResult, PermissionUtil.isSpecialPermission(permissions[index]))
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "handlePermissionsResult: requestCode = $requestCode, permissionResults = $permissionResults")
        }else{
            LogUtil.d(TAG, "handlePermissionsResult: permission result callback is empty, requestCode = $requestCode, permissions = $permissions")
        }
        launchedLaunchers[requestCode]?.unregister()
        launchedLaunchers.remove(requestCode)
    }

    private fun registerForRequestNormalPermissionsResult(requestCode: Int): ActivityResultLauncher<Array<String>> {
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleRequestNormalPermissionsResultCallback(requestCode)
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.RequestMultiplePermissions(), resultCallback)
        launchedLaunchers.put(requestCode, launcher)
        return launcher
    }

    private fun registerForRequestSpecialPermissionsResult(requestCode: Int): ActivityResultLauncher<Intent>{
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleRequestSpecialPermissionsResultCallback(requestCode)
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        launchedLaunchers.put(requestCode, launcher)
        return launcher
    }

    private fun registerForCheckPermissionsResult(requestCode: Int): ActivityResultLauncher<Intent> {
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleCheckPermissionsResultCallback(requestCode)
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        launchedLaunchers.put(requestCode, launcher)
        return launcher
    }

    /**
     * 由于带lifecycle的[androidx.activity.result.ActivityResultRegistry.register]方法需要在STARTED之前调用，不符合当前场景
     * 所以使用不带lifecycle的[androidx.activity.result.ActivityResultRegistry.register]方法注册回调，需要自己处理以下两个场景：
     * 1、配置更改后Fragment销毁重建时的数据保存和恢复
     * 2、只在Fragment可见时才进行结果回调
     */
    private fun <I, O> registerForResultsWithNotLifecycle(key: Int, contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
        return registry.register(key.toString(), contract, callback)
    }

    private fun requestSpecialPermission(launcher: ActivityResultLauncher<Intent>, permission: String){
        launcher.launch(SpecialUtil.getIntent(host, permission))
    }

    inner class HandleRequestNormalPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<Map<String, Boolean>>{

        override fun onActivityResult(result: Map<String, Boolean>) {
            val permissions = Array(result.size){""}
            val grantResults = BooleanArray(result.size)
            result.keys.forEachIndexed{ index, name ->
                permissions[index] = name
                grantResults[index] = result[name] ?: error("permission result is null, name = $name")
            }
            requestNormalPermissionsLaunchedKeys.remove(requestCode)
            requestNormalPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
        }
    }

    inner class HandleRequestSpecialPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<ActivityResult>{

        override fun onActivityResult(result: ActivityResult?) {
            if(waitForCheckSpecialPermissions.containKey(requestCode)){
                val specialPermissions = waitForCheckSpecialPermissions[requestCode]
                specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
                requestSpecialPermissionsResultLiveData.value = PermissionsResult(requestCode, specialPermissions.getPermissions(), specialPermissions.getGrantResults())
            }
        }
    }

    inner class HandleCheckPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<ActivityResult>{

        override fun onActivityResult(result: ActivityResult?) {
            if(waitForCheckPermissions.containKey(requestCode)){
                val permissions = waitForCheckPermissions[requestCode]
                val grantResults = BooleanArray(permissions.size)
                permissions.forEachIndexed { index, permission ->
                    grantResults[index] = if(PermissionUtil.isSpecialPermission(permission)){
                        PermissionUtil.checkSpecialPermission(host, permission)
                    }else{
                        PermissionUtil.checkNormalPermission(host, permission)
                    }
                }
                waitForCheckPermissions.remove(requestCode)
                checkPermissionsLaunchedKeys.remove(requestCode)
                checkPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}