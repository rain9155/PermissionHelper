package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.example.permission.base.AbsProxyFragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.*
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SpecialUtil
import kotlin.collections.ArrayList
import com.example.permission.proxy.ProxyFragmentViewModel.*

/**
 * 申请权限的代理Fragment, 具有生命周期感应，只在Fragment可见时才把结果回调，同时当配置更改销毁重建时也可以恢复状态
 * Created by 陈健宇 at 2019/3/25
 */
internal class ProxyFragmentV1 : AbsProxyFragment() {

    companion object {
        private const val TAG = "ProxyFragmentV1"
        private const val INITIAL_REQUEST_CODE = 0x0000100

        fun newInstance(): ProxyFragmentV1 {
            return ProxyFragmentV1()
        }
    }

    private val host = requestActivity()
    private val viewModel: ProxyFragmentV1ViewModel = getViewModel(this, ProxyFragmentV1ViewModel::class.java)
    private val permissionResultCallbacks = viewModel.permissionResultCallbacks
    private val waitForCheckPermissions = viewModel.waitForCheckPermissions
    private val waitForCheckSpecialPermissions = viewModel.waitForCheckSpecialPermissions
    private val requestNormalPermissionsResultLiveData = viewModel.requestNormalPermissionsResultLiveData
    private val requestSpecialPermissionsResultLiveData = viewModel.requestSpecialPermissionsResultLiveData
    private val checkPermissionsResultLiveData = viewModel.checkPermissionsResultLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNormalPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }
        requestSpecialPermissionsResultLiveData.observe(this) { result ->
            val requestCode = result.requestCode
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            if(specialPermissions.hasNext()){
                requestSpecialPermission(specialPermissions.nextPermission(), requestCode)
            }else{
                waitForCheckSpecialPermissions.remove(requestCode)
                handlePermissionsResult(requestCode, result.permissions, result.grantResults)
            }
        }
        checkPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults)}
    }

    override fun requestActivity(): Activity {
        return requireActivity()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestNormalPermissions(permissions.toTypedArray(), callback)
    }

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback){
        requestSpecialPermissions(permissions.toTypedArray(), callback)
    }

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) {
        startSettingsForCheckResults(permissions.toTypedArray(), callback)
    }

    override fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "requestNormalPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        requestPermissions(permissions, requestCode)
    }

    override fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        waitForCheckSpecialPermissions.put(requestCode, SpecialArray(permissions))
        requestSpecialPermission(waitForCheckSpecialPermissions[requestCode].nextPermission(), requestCode)
    }

    override fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        waitForCheckPermissions.put(requestCode, permissions)
        startActivityForResult(SettingsUtil.getIntent(host), requestCode)
    }

    override fun generateRequestCode(): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = INITIAL_REQUEST_CODE)
        } while (permissionResultCallbacks.containKey(requestCode))
        return requestCode
    }

    override fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray) {
        val callback = permissionResultCallbacks[requestCode]
        if (callback != null) {
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed { index, grantResult ->
                permissionResults[index] = PermissionResult(permissions[index], grantResult)
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "handlePermissionsResult: requestCode = $requestCode, permissionResults = $permissionResults")
        }else{
            LogUtil.d(TAG, "handlePermissionsResult: permission result callback is empty, requestCode = $requestCode, permissions = $permissions")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val grantedResult = BooleanArray(permissions.size)
        grantResults.forEachIndexed { index, grantResult ->
            grantedResult[index] = grantResult == PackageManager.PERMISSION_GRANTED
        }
        requestNormalPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantedResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(waitForCheckSpecialPermissions.containKey(requestCode)){
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
            requestSpecialPermissionsResultLiveData.value = PermissionsResult(requestCode, specialPermissions.getPermissions(), specialPermissions.getGrantResults())
        }else if(waitForCheckPermissions.containKey(requestCode)){
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
            checkPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestSpecialPermission(permission: String, requestCode: Int){
        startActivityForResult(SpecialUtil.getIntent(host, permission), requestCode)
    }

}