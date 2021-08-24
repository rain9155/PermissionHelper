package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import androidx.annotation.RequiresApi
import com.example.permission.base.AbsProxyFragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SettingsUtil
import com.example.permission.utils.SpecialUtil

/**
 * 申请权限的代理Fragment
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val callback = permissionResultCallbacks[requestCode]
        if (callback != null) {
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed { index, grantResult ->
                permissionResults[index] = PermissionResult(permissions[index], grantResult == PackageManager.PERMISSION_GRANTED)
            }
            callback.onPermissionResults(permissionResults)
            LogUtil.d(TAG, "onRequestPermissionsResult: requestCode = $requestCode, permissionResults = $permissionResults")
        }else{
            LogUtil.d(TAG, "onRequestPermissionsResult: permission result callback is empty, requestCode = $requestCode, permissions = $permissions")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val permissions = waitForCheckPermissions[requestCode]
        if(permissions != null){
            waitForCheckPermissions.remove(requestCode)
            val callback = permissionResultCallbacks[requestCode]
            if(callback != null){
                permissionResultCallbacks.remove(requestCode)
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
                LogUtil.d(TAG, "onActivityResult: requestCode = $requestCode, permissionResults = $permissionResults")
            }else{
                LogUtil.d(TAG, "onActivityResult: permission result callback is empty, requestCode = $requestCode, permissions = $permissions")
            }
            return
        }

        val specialPermissions = waitForCheckSpecialPermissions[requestCode]
        specialPermissions.setPriorGrantResult(PermissionUtil.checkSpecialPermission(host, specialPermissions.priorPermission()))
        if(specialPermissions.hasNext()){
            requestSpecialPermission(specialPermissions.nextPermission(), requestCode)
        }else{
            waitForCheckSpecialPermissions.remove(requestCode)
            val callback = permissionResultCallbacks[requestCode]
            if (callback != null) {
                permissionResultCallbacks.remove(requestCode)
                val permissionResults = ArrayList<PermissionResult>(specialPermissions.size())
                specialPermissions.getGrantResults().forEachIndexed { index, granted ->
                    permissionResults[index] = PermissionResult(specialPermissions.get(index), granted, special = true)
                }
                callback.onPermissionResults(permissionResults)
                LogUtil.d(TAG, "onActivityResult: requestCode = $requestCode, permissionResults = $permissionResults")
            }else{
                LogUtil.d(TAG, "onActivityResult: permission result callback is empty, requestCode = $requestCode, permissions = ${specialPermissions.getPermissions()}")
            }
        }
    }

    private fun requestSpecialPermission(permission: String, requestCode: Int){
        startActivityForResult(SpecialUtil.getIntent(host, permission), requestCode)
    }

    private fun generateRequestCode(): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = INITIAL_REQUEST_CODE)
        } while (permissionResultCallbacks.indexOfKey(requestCode) >= 0)
        return requestCode
    }

}