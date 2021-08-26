package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        private const val MSG_FINISH_REQUEST_NORMAL_PERMISSIONS = 0x0000
        private const val MSG_FINISH_REQUEST_SPECIAL_PERMISSIONS = 0x0001
        private const val MSG_FINISH_CHECK_PERMISSIONS = 0x0002

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

    private val removeHandler = Handler(Looper.getMainLooper()) { message ->
        val requestCode = message.obj as Int
        launchedLaunchers[requestCode]?.unregister()
        launchedLaunchers.remove(requestCode)
        permissionResultCallbacks.remove(requestCode)
        when(message.what){
            MSG_FINISH_REQUEST_NORMAL_PERMISSIONS -> {
                requestNormalPermissionsLaunchedKeys.remove(requestCode)
            }
            MSG_FINISH_REQUEST_SPECIAL_PERMISSIONS -> {
                requestSpecialPermissionsLaunchedKeys.remove(requestCode)
                waitForCheckSpecialPermissions.remove(requestCode)
            }
            MSG_FINISH_CHECK_PERMISSIONS -> {
                checkPermissionsLaunchedKeys.remove(requestCode)
                waitForCheckPermissions.remove(requestCode)
            }
            else -> {
                return@Handler false
            }
        }
        return@Handler true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(requestSpecialPermissionsLaunchedKeys.isNotEmpty()){
            requestSpecialPermissionsLaunchedKeys.forEach { requestCode ->
                registerForRequestNormalPermissionsResult(requestCode, permissionResultCallbacks[requestCode])
            }
        }

        if(requestSpecialPermissionsLaunchedKeys.isNotEmpty()){
            requestSpecialPermissionsLaunchedKeys.forEach { requestCode ->
                registerForRequestSpecialPermissionsResult(requestCode, waitForCheckSpecialPermissions[requestCode], permissionResultCallbacks[requestCode])
            }
        }

        if(checkPermissionsLaunchedKeys.isNotEmpty()){
            checkPermissionsLaunchedKeys.forEach { requestCode ->
                registerForCheckPermissionsResult(requestCode, waitForCheckPermissions[requestCode], permissionResultCallbacks[requestCode])
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        launchedLaunchers.forEach { launcher ->
            launcher.unregister()
        }
        launchedLaunchers.clear()
        removeHandler.removeCallbacksAndMessages(null)
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
        val launcher = registerForRequestNormalPermissionsResult(requestCode, callback)
        requestNormalPermissionsLaunchedKeys.add(requestCode)
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
        val launcher = registerForRequestSpecialPermissionsResult(requestCode, specialPermissions, callback)
        requestSpecialPermissionsLaunchedKeys.add(requestCode)
        requestSpecialPermission(launcher, specialPermissions.nextPermission())
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val launcher = registerForCheckPermissionsResult(requestCode, permissions, callback)
        checkPermissionsLaunchedKeys.add(requestCode)
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

    private fun registerForRequestNormalPermissionsResult(requestCode: Int, callback: IPermissionResultsCallback): ActivityResultLauncher<Array<String>> {
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleRequestNormalPermissionsResultCallback(
            requestCode,
            callback
        )
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.RequestMultiplePermissions(), resultCallback)
        launchedLaunchers.put(requestCode, launcher)
        permissionResultCallbacks.put(requestCode, callback)
        return launcher
    }

    private fun registerForRequestSpecialPermissionsResult(requestCode: Int, specialPermissions: SpecialArray, callback: IPermissionResultsCallback): ActivityResultLauncher<Intent>{
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleRequestSpecialPermissionsResultCallback(
            requestCode,
            specialPermissions,
            ::requestSpecialPermission,
            callback
        )
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        resultCallback.launcher = launcher
        launchedLaunchers.put(requestCode, launcher)
        waitForCheckSpecialPermissions.put(requestCode, specialPermissions)
        permissionResultCallbacks.put(requestCode, callback)
        return launcher
    }

    private fun registerForCheckPermissionsResult(requestCode: Int, permissions: Array<String>, callback: IPermissionResultsCallback): ActivityResultLauncher<Intent> {
        if(launchedLaunchers.containKey(requestCode)){
            launchedLaunchers[requestCode].unregister()
        }
        val resultCallback = HandleCheckPermissionsResultCallback(
            requestCode,
            permissions,
            callback
        )
        val launcher = registerForResultsWithNotLifecycle(requestCode, ActivityResultContracts.StartActivityForResult(), resultCallback)
        launchedLaunchers.put(requestCode, launcher)
        waitForCheckPermissions.put(requestCode, permissions)
        permissionResultCallbacks.put(requestCode, callback)
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

    private fun requestSpecialPermission(launcher: ActivityResultLauncher<Intent>?, permission: String){
        launcher?.launch(SpecialUtil.getIntent(host, permission))
    }

    inner class HandleRequestNormalPermissionsResultCallback(
        private val requestCode: Int,
        private val callback: IPermissionResultsCallback
    ) : ActivityResultCallback<Map<String, Boolean>>{

        override fun onActivityResult(result: Map<String, Boolean>) {
            removeHandler.obtainMessage(MSG_FINISH_REQUEST_NORMAL_PERMISSIONS, requestCode).sendToTarget()
            val permissionResults = ArrayList<PermissionResult>(result.size)
            result.keys.forEachIndexed{ index, name ->
                permissionResults[index] = PermissionResult(name, result[name] ?: error("permission name is null"))
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onRequestNormalPermissionsResult: permissionResults = $permissionResults")
        }
    }

    inner class HandleRequestSpecialPermissionsResultCallback(
        private val requestCode: Int,
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
                removeHandler.obtainMessage(MSG_FINISH_REQUEST_SPECIAL_PERMISSIONS, requestCode).sendToTarget()
                val permissionResults = ArrayList<PermissionResult>(specialPermissions.size())
                specialPermissions.getGrantResults().forEachIndexed { index, granted ->
                    permissionResults[index] = PermissionResult(specialPermissions.get(index), granted, special = true)
                }
                callback.onPermissionResults(permissionResults)
                LogUtil.d(TAG, "onRequestSpecialPermissionsResult: permissionResults = $permissionResults")
            }
        }
    }

    inner class HandleCheckPermissionsResultCallback(
        private val requestCode: Int,
        private val permissions: Array<String>,
        private val callback: IPermissionResultsCallback
    ) : ActivityResultCallback<ActivityResult>{

        override fun onActivityResult(result: ActivityResult?) {
            removeHandler.obtainMessage(MSG_FINISH_CHECK_PERMISSIONS, requestCode).sendToTarget()
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