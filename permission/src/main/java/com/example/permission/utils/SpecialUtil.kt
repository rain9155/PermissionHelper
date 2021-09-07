package com.example.permission.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.example.permission.PermissionHelper

/**
 * 统一管理各种特殊权限的工具类
 * Created by 陈健宇 at 2019/6/2
 */
internal object SpecialUtil {

    private const val TAG = "SpecialUtil"
    private val specialPermissions = listOf(
        Manifest.permission.WRITE_SETTINGS,//允许应用修改系统设置
        Manifest.permission.SYSTEM_ALERT_WINDOW,//允许应用显示在其他应用上面
        Manifest.permission.REQUEST_INSTALL_PACKAGES,//允许应用安装未知来源应用
        Manifest.permission.PACKAGE_USAGE_STATS,//允许应用收集其他应用的使用信息
        Manifest.permission.MANAGE_EXTERNAL_STORAGE//允许应用访问作用域存储(scoped storage)中的外部存储
    )

    /**
     * 判断是否是特殊权限
     */
    fun isSpecialPermission(permission: String): Boolean{
        return specialPermissions.contains(permission)
    }

    /**
     * 返回特殊权限对应的Settings设置界面的Intent, 有可能会不存在对应的activity，启动时需要try catch，
     */
    @SuppressLint("InlinedApi", "QueryPermissionsNeeded")
    fun getIntent(context: Context, permission: String): Intent{
        val action = when(permission){
            Manifest.permission.WRITE_SETTINGS -> {
                Settings.ACTION_MANAGE_WRITE_SETTINGS
            }
            Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            }
            Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
            }
            Manifest.permission.PACKAGE_USAGE_STATS -> {
                Settings.ACTION_USAGE_ACCESS_SETTINGS
            }
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            }
            else ->{
                LogUtil.d(TAG, "getIntent: unknown special permission, permission = $permission")
                SettingsUtil.getAppDetailIntent(context).action
            }
        }
        val intent = Intent(action, Uri.parse("package:${context.packageName}"))
        if(intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY) != null){
            return intent
        }
        return intent.apply {
            data = null
        }
    }

    /**
     * 检查特殊权限是否被授予
     */
    fun checkPermission(context: Context, permission: String): Boolean{
        return when(permission){
            Manifest.permission.WRITE_SETTINGS -> {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)
            }
            Manifest.permission.SYSTEM_ALERT_WINDOW -> {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
            }
            Manifest.permission.REQUEST_INSTALL_PACKAGES -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    false
                }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    true
                }else{
                    context.packageManager.canRequestPackageInstalls()
                }
            }
            Manifest.permission.PACKAGE_USAGE_STATS -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                    false
                }else{
                    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    val mode = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),context.packageName)
                    }else{
                        appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(),context.packageName)
                    }
                    mode == AppOpsManager.MODE_ALLOWED
                }
            }
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
                    false
                }else{
                    Environment.isExternalStorageManager()
                }
            }
            else ->{
                LogUtil.d(TAG, "checkPermission: unknown special permission, permission = $permission")
                false
            }
        }
    }

}