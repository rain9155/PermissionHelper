package com.example.permission.proxy

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
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
        requestNormalPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults) }
        requestSpecialPermissionsResultLiveData.observe(this) { result ->
            val requestCode = result.requestCode
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            if(specialPermissions != null && specialPermissions.hasNext()){
                requestSpecialPermission(specialPermissions.nextPermission(), requestCode)
            }else{
                waitForCheckSpecialPermissions.remove(requestCode)
                handlePermissionsResult(requestCode, result.permissions, result.grantResults)
            }
        }
        checkPermissionsResultLiveData.observe(this) { result -> handlePermissionsResult(result.requestCode, result.permissions, result.grantResults)}
    }

    override fun createViewModel(): ProxyFragmentV1ViewModel {
        return getViewModel(this, ProxyFragmentV1ViewModel::class.java)
    }

    override fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback) {
        LogUtil.d(TAG, "requestNormalPermissions: permissions = ${permissions.toStrings()}")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        val requestCode = generateRequestCode()
        permissionResultCallbacks.put(requestCode, callback)
        requestPermissions(permissions, requestCode)
    }

    override fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = ${permissions.toStrings()}")
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
        LogUtil.d(TAG, "startSettingsActivityForResults: permissions = ${permissions.toStrings()}")
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
            requestCode = PermissionUtil.generateRandomCode(initialCode = 0, maxCode = MAX_REQUEST_CODE)
        } while (permissionResultCallbacks.containKey(requestCode))
        return requestCode
    }

    override fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray) {
        val callback = permissionResultCallbacks[requestCode]
        if (callback != null) {
            LogUtil.d(TAG, "handlePermissionsResult: requestCode = $requestCode, permissions = ${permissions.toStrings()}, grantResults = ${grantResults.toStrings()}")
            permissionResultCallbacks.remove(requestCode)
            val permissionResults = ArrayList<PermissionResult>(permissions.size)
            grantResults.forEachIndexed { index, grantResult ->
                permissionResults.add(PermissionResult(permissions[index], grantResult))
            }
            callback.onPermissionResults(permissionResults)
        }else{
            LogUtil.d(TAG, "handlePermissionsResult: permission result callback is empty, requestCode = $requestCode, permissions = ${permissions.toStrings()}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.d(TAG, "onRequestPermissionsResult: requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults")
        val grantedResult = BooleanArray(permissions.size)
        grantResults.forEachIndexed { index, grantResult ->
            grantedResult[index] = grantResult == PackageManager.PERMISSION_GRANTED
        }
        requestNormalPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantedResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        LogUtil.d(TAG, "onActivityResult: requestCode = $requestCode, data = $data")
        if(waitForCheckSpecialPermissions.containKey(requestCode)){
            val specialPermissions = waitForCheckSpecialPermissions[requestCode]
            specialPermissions.setPriorGrantResult(SpecialUtil.checkPermission(host, specialPermissions.priorPermission()))
            requestSpecialPermissionsResultLiveData.value = PermissionsResult(requestCode, specialPermissions.getPermissions(), specialPermissions.getGrantResults())
        }else if(waitForCheckPermissions.containKey(requestCode)){
            val permissions = waitForCheckPermissions[requestCode]
            val grantResults = BooleanArray(permissions.size)
            permissions.forEachIndexed { index, permission ->
                grantResults[index] = PermissionUtil.checkPermission(host, permission)
            }
            waitForCheckPermissions.remove(requestCode)
            checkPermissionsResultLiveData.value = PermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun requestSpecialPermission(permission: String, requestCode: Int){
        startActivityForResult(SpecialUtil.getIntent(host, permission), requestCode)
    }

}