package com.example.permission.proxy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
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
internal class ProxyFragmentV2 : AbsProxyFragment<ProxyFragmentV2ViewModel>(){

    companion object {
        private const val TAG = "ProxyFragmentV2"
        private const val INITIAL_REQUEST_CODE = 0x00010000

        fun newInstance(): ProxyFragmentV2 {
            return ProxyFragmentV2()
        }
    }

    private lateinit var registry: ActivityResultRegistry
    private lateinit var requestNormalPermissionsLaunchedKeys: MutableList<Int>
    private lateinit var requestSpecialPermissionsLaunchedKeys: MutableList<Int>
    private lateinit var checkPermissionsLaunchedKeys: MutableList<Int>
    private val launchedLaunchers = SparseArray<ActivityResultLauncher<*>>()

    override fun createViewModel(): ProxyFragmentV2ViewModel {
        return getViewModel(this, ProxyFragmentV2ViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registry = host.activityResultRegistry
        viewModel.run {
            this@ProxyFragmentV2.requestNormalPermissionsLaunchedKeys = requestNormalPermissionsLaunchedKeys
            this@ProxyFragmentV2.requestSpecialPermissionsLaunchedKeys = requestSpecialPermissionsLaunchedKeys
            this@ProxyFragmentV2.checkPermissionsLaunchedKeys = checkPermissionsLaunchedKeys
        }

        requestNormalPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }
        requestSpecialPermissionsResultLiveData.observe(this) { result ->
            val requestCode = result.requestCode
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            if(specialPermissions != null && specialPermissions.hasNext()){
                val launcher = launchedLaunchers[requestCode] as ActivityResultLauncher<Intent>
                requestSpecialPermission(launcher, specialPermissions.nextPermission(), requestCode)
            }else{
                waitForCheckSpecialPermissions.remove(requestCode)
                requestSpecialPermissionsLaunchedKeys.remove(requestCode)
                handlePermissionsResult(requestCode, result.permissions, result.grantResults)
            }
        }
        checkPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }

        requestNormalPermissionsLaunchedKeys.forEach { requestCode -> registerForRequestNormalPermissionsResult(requestCode) }
        requestSpecialPermissionsLaunchedKeys.forEach { requestCode -> registerForRequestSpecialPermissionsResult(requestCode) }
        checkPermissionsLaunchedKeys.forEach { requestCode -> registerForCheckPermissionsResult(requestCode) }
    }

    override fun onDestroy() {
        super.onDestroy()
        launchedLaunchers.forEach { launcher -> launcher.unregister() }
        launchedLaunchers.clear()
    }

    override fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray) {
        super.handlePermissionsResult(requestCode, permissions, grantResults)
        val callback = permissionResultCallbacks[requestCode]
        if(callback != null){
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed{ index, grantResult ->
                permissionResults.add(PermissionResult(permissions[index], grantResult, SpecialUtil.isSpecialPermission(permissions[index])))
            }
            callback.onPermissionResults(permissionResults)
        }
        launchedLaunchers[requestCode]?.unregister()
        launchedLaunchers.remove(requestCode)
    }

    override fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        super.requestNormalPermissions(permissions, callback)
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
        super.requestSpecialPermissions(permissions, callback)
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
        requestSpecialPermission(launcher, specialPermissions.nextPermission(), requestCode)
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback) {
        super.startSettingsForCheckResults(permissions, callback)
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        val launcher = registerForCheckPermissionsResult(requestCode)
        checkPermissionsLaunchedKeys.add(requestCode)
        waitForCheckPermissions.put(requestCode, permissions)
        permissionResultCallbacks.put(requestCode, callback)
        try {
            launcher.launch(SettingsUtil.getIntent(host))
        }catch (e: ActivityNotFoundException){
            LogUtil.e(TAG, "startSettingsForCheckResults: e = $e")
            handleCheckPermissionsResult(requestCode)
        }
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
        LogUtil.d(TAG, "registerForResultsWithNotLifecycle: key = $key")
        return registry.register(key.toString(), contract, callback)
    }

    private fun requestSpecialPermission(launcher: ActivityResultLauncher<Intent>, permission: String, requestCode: Int){
        try {
            launcher.launch(SpecialUtil.getIntent(host, permission))
        }catch (e: ActivityNotFoundException){
            LogUtil.e(TAG, "requestSpecialPermission: e = $e")
            handleRequestSpecialPermissionResult(requestCode)
        }
    }

    private fun handleRequestSpecialPermissionResult(requestCode: Int) {
        val specialPermissions = waitForCheckSpecialPermissions[requestCode]
        specialPermissions.setPriorGrantResult(SpecialUtil.checkPermission(host, specialPermissions.priorPermission()))
        requestSpecialPermissionsResultLiveData.value = PermissionsResult(requestCode, specialPermissions.getPermissions(), specialPermissions.getGrantResults())
    }

    private fun handleCheckPermissionsResult(requestCode: Int) {
        val permissions = waitForCheckPermissions[requestCode]
        val grantResults = BooleanArray(permissions.size)
        permissions.forEachIndexed { index, permission ->
            grantResults[index] = PermissionUtil.checkPermission(host, permission)
        }
        waitForCheckPermissions.remove(requestCode)
        checkPermissionsLaunchedKeys.remove(requestCode)
        checkPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
    }

    private inner class HandleRequestNormalPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<Map<String, Boolean>>{

        override fun onActivityResult(result: Map<String, Boolean>) {
            LogUtil.d(TAG, "onHandleRequestNormalPermissionsResultCallback: result = $result")
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

    private inner class HandleRequestSpecialPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<ActivityResult>{

        override fun onActivityResult(result: ActivityResult?) {
            LogUtil.d(TAG, "onHandleRequestSpecialPermissionsResultCallback: result = $result")
            if(waitForCheckSpecialPermissions.containKey(requestCode)){
                handleRequestSpecialPermissionResult(requestCode)
            }
        }
    }

    private inner class HandleCheckPermissionsResultCallback(private val requestCode: Int) : ActivityResultCallback<ActivityResult>{

        override fun onActivityResult(result: ActivityResult?) {
            LogUtil.d(TAG, "onHandleCheckPermissionsResultCallback: result = $result")
            if(waitForCheckPermissions.containKey(requestCode)){
                handleCheckPermissionsResult(requestCode)
            }
        }
    }
}