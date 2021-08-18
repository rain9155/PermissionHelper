package com.example.permission.base

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity

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