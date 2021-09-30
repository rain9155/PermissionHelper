package com.example.permission.proxy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.Observer
import com.example.permission.base.*
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PermissionResult
import com.example.permission.base.ProxyFragmentV1ViewModel
import com.example.permission.utils.*
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SpecialUtil
import kotlin.collections.ArrayList
import com.example.permission.base.SpecialArray

/**
 * 申请权限的代理Fragment, 具有生命周期感应，只在Fragment可见时才把结果回调，同时当配置更改销毁重建时也可以恢复状态
 * Created by 陈健宇 at 2019/3/25
 */
internal class ProxyFragmentV1 : AbsProxyFragment<ProxyFragmentV1ViewModel>() {

    companion object {
        private const val TAG = "ProxyFragmentV1"

        /**
         * 在FragmentActivity中限制了requestCode的取值范围为[0, 65535], https://stackoverflow.com/questions/25529865/java-lang-illegalargumentexception-can-only-use-lower-16-bits-for-requestcode
         */
        private const val MAX_REQUEST_CODE = 0x00010000 - 1

        fun newInstance(): ProxyFragmentV1 {
            return ProxyFragmentV1()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * 这里需要注意LivData的数据倒灌问题，原因和解决办法：https://juejin.cn/post/6986895858239275015
         */
        requestNormalPermissionsResultLiveData.observe(this, Observer { result ->
            handlePermissionsResult(result.requestCode, result.permissions, result.grantResults)
        })
        requestSpecialPermissionsResultLiveData.observe(this, Observer { result ->
            val requestCode = result.requestCode
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            if(specialPermissions != null && specialPermissions.hasNext()){
                requestSpecialPermission(specialPermissions.nextPermission(), requestCode)
            }else{
                waitForCheckSpecialPermissions.remove(requestCode)
                handlePermissionsResult(requestCode, result.permissions, result.grantResults)
            }
        })
        checkPermissionsResultLiveData.observe(this, Observer { result ->
            handlePermissionsResult(result.requestCode, result.permissions, result.grantResults)
        })
    }

    override fun createViewModel(): ProxyFragmentV1ViewModel {
        return getViewModel(this, ProxyFragmentV1ViewModel::class.java)
    }

    override fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray) {
        super.handlePermissionsResult(requestCode, permissions, grantResults)
        val callback = permissionResultCallbacks[requestCode]
        if (callback != null) {
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed { index, grantResult ->
                val permission = permissions[index]
                val special = SpecialUtil.isSpecialPermission(permission)
                val should = PermissionUtil.checkShouldShowRationale(requestActivity(), permission)
                permissionResults.add(PermissionResult(permission, grantResult, special, should))
            }
            callback.onPermissionResults(permissionResults)
        }
    }

    override fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback) {
        super.requestNormalPermissions(permissions, callback)
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        requestPermissions(permissions, requestCode)
    }

    override fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        super.requestSpecialPermissions(permissions, callback)
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
        super.startSettingsForCheckResults(permissions, callback)
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        waitForCheckPermissions.put(requestCode, permissions)
        try {
            startActivityForResult(SettingsUtil.getIntent(host), requestCode)
        }catch (e: ActivityNotFoundException){
            LogUtil.e(TAG, "startSettingsForCheckResults: e = $e")
            handleCheckPermissionsResult(requestCode)
        }
    }

    override fun generateRequestCode(): Int{
        var requestCode: Int
        do {
            requestCode = PermissionUtil.generateRandomCode(initialCode = 0, maxCode = MAX_REQUEST_CODE)
        } while (permissionResultCallbacks.containKey(requestCode))
        return requestCode
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
            handleRequestSpecialPermissionResult(requestCode)
        }else if(waitForCheckPermissions.containKey(requestCode)){
            handleCheckPermissionsResult(requestCode)
        }
    }

    private fun requestSpecialPermission(permission: String, requestCode: Int){
        try {
            startActivityForResult(SpecialUtil.getIntent(host, permission), requestCode)
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
        checkPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
    }
}