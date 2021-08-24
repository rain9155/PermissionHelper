package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.util.SparseArray
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.example.permission.base.AbsProxyFragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SettingsUtil
import com.example.permission.utils.SpecialUtil
import java.util.concurrent.atomic.AtomicInteger

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
    private val requestPermissionsLaunchers = viewModel.requestPermissionsLaunchers
    private val startActivityLaunchers = viewModel.startActivityLaunchers
    private val nextLocalRequestCode = viewModel.nextLocalRequestCode

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
        val requestCode = generateRequestCode(requestPermissionsLaunchers)
        val normalPermissionsLauncher = registerForResults(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            requestPermissionsLaunchers[requestCode].unregister()
            requestPermissionsLaunchers.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(results.size)
            results.keys.forEachIndexed{ index, name ->
                permissionResults[index] = PermissionResult(name, results[name]!!)
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onRequestPermissionsResult: permissionResults = $permissionResults")
        }
        requestPermissionsLaunchers.put(requestCode, normalPermissionsLauncher)
        normalPermissionsLauncher.launch(permissions)
    }

    override fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode(startActivityLaunchers)
        val specialPermissions = SpecialArray(permissions)
        val requestSpecialPermission = {launcher: ActivityResultLauncher<Intent>, permission: String ->
            launcher.launch(SpecialUtil.getIntent(host, permission))
        }
        val specialPermissionLauncher = registerForResults(ActivityResultContracts.StartActivityForResult()) {
            specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
            val specialPermissionLauncher = startActivityLaunchers[requestCode]
            if(specialPermissions.hasNext()){
                requestSpecialPermission.invoke(specialPermissionLauncher, specialPermissions.nextPermission())
            }else{
                specialPermissionLauncher.unregister()
                startActivityLaunchers.remove(requestCode)
                val permissionResults = ArrayList<PermissionResult>(specialPermissions.size())
                specialPermissions.getGrantResults().forEachIndexed { index, granted ->
                    permissionResults[index] = PermissionResult(specialPermissions.get(index), granted, special = true)
                }
                callback.onPermissionResults(permissionResults)
                LogUtil.d(TAG, "onStartActivityForResults: permissionResults = $permissionResults")
            }
        }
        startActivityLaunchers.put(requestCode, specialPermissionLauncher)
        requestSpecialPermission.invoke(specialPermissionLauncher, specialPermissions.nextPermission())
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode(startActivityLaunchers)
        val settingsLauncher = registerForResults(ActivityResultContracts.StartActivityForResult()){
            startActivityLaunchers[requestCode].unregister()
            startActivityLaunchers.remove(requestCode)
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
        startActivityLaunchers.put(requestCode, settingsLauncher)
        settingsLauncher.launch(SettingsUtil.getIntent(host))
    }

    private fun <I, O> registerForResults(contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
        return registry.register(generateKey(), this, contract, callback)
    }

    private fun generateKey(): String{
        return "$TAG#${nextLocalRequestCode.getAndIncrement()}"
    }

    private fun generateRequestCode(list: SparseArray<*>): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = INITIAL_REQUEST_CODE)
        } while (list.indexOfKey(requestCode) >= 0)
        return requestCode
    }
}