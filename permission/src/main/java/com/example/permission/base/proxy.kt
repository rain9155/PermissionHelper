package com.example.permission.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.permission.proxy.ProxyFragmentV1
import com.example.permission.utils.LogUtil
import com.example.permission.utils.PermissionUtil

/**
 * 内部统一权限回调处理、接口定义
 * Created by 陈健宇 at 2021/8/15
 */

/**
 * 封装申请后的权限信息
 */
internal data class PermissionResult(
    val name: String,
    val granted: Boolean,
    val special: Boolean = false,
)

/**
 * 权限申请后的回调
 */
internal interface IPermissionResultsCallback {

    fun onPermissionResults(permissionResults: List<PermissionResult>)

}

/**
 * 申请权限的代理Fragment的公共接口
 */
internal interface IProxyFragment {

    fun requestActivity(): Activity

    fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback)

    fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback)

    fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback)

}

/**
 * 代理fragment的公共实现
 */
internal abstract class AbsProxyFragment : Fragment(), IProxyFragment {

    companion object{
        private const val TAG = "AbsProxyFragment"
    }

    protected abstract fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun startSettingsForCheckResults(permissions: Array<String>, callback: IPermissionResultsCallback)

    protected abstract fun generateRequestCode(): Int

    protected abstract fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: BooleanArray)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.d(TAG, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        LogUtil.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        LogUtil.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        LogUtil.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d(TAG, "onDestroy")
    }
}