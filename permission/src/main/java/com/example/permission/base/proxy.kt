package com.example.permission.base

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle

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
 * 代理fragment销毁重建后的实例更新通知回调
 */
internal interface IProxyFragmentUpdateCallback {

    fun onProxyFragmentUpdate(proxyFragment: IProxyFragment)

}



/**
 * 申请权限的代理Fragment的公共接口
 */
internal interface IProxyFragment {

    fun requestActivity(): FragmentActivity

    fun requestFragmentManager(): FragmentManager

    fun obtainLifecycle(): Lifecycle

    fun obtainFragmentUpdateCallbackManager(): FragmentUpdateCallbackManager

    fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback)

    fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback)

    fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback)

    /**
     * IProxyFragmentUpdateCallback会保存在代理Fragment的ViewModel中，注意内存泄漏
     */
    interface FragmentUpdateCallbackManager{

        fun add(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean

        fun remove(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean

        fun contain(fragmentUpdateCallback: IProxyFragmentUpdateCallback): Boolean

    }

}