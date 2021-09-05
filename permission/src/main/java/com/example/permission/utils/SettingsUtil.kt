package com.example.permission.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import java.util.*

/**
 * 获取各厂商的权限界面工具类
 * Created by 陈健宇 at 2019/6/3
 */
internal object SettingsUtil {

    /**
     * 获取不同厂商的权限设置界面的intent，不满足条件时返回应用详情界面的intent
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun getIntent(context: Context): Intent{
        val brand = Build.BRAND.toLowerCase(Locale.ROOT)
        val intent = if (TextUtils.equals(brand, "redmi") || TextUtils.equals(brand, "xiaomi")) {
            getMEIZUIntent(context)
        } else if (TextUtils.equals(brand, "meizu")) {
            getMEIZUIntent(context)
        } else if (TextUtils.equals(brand, "huawei") || TextUtils.equals(brand, "honor")) {
            getHUAWEIIntent()
        } else if(TextUtils.equals(brand, "oppo")){
            getOPPOIntent()
        }else {
            getAppDetailIntent(context)
        }
        return if(intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY) != null){
            intent
        }else{
            getAppDetailIntent(context)
        }
    }

    /**
     * 获取应用详情界面的intent
     */
    fun getAppDetailIntent(context: Context): Intent{
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
    }

    /**
     * 获取华为权限管理页面的intent
     */
    fun getHUAWEIIntent(): Intent{
        return Intent().apply {
            ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity")
        }
    }


    /**
     * 获取小米的权限管理页面的intent
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun getMIUI8Intent(context: Context): Intent{
        // MIUI 8
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            putExtra("extra_pkgname", context.packageName)
            component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        }
        return if(intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY) != null){
            intent
        }else{
            // MIUI 5/6/7
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
            }
        }
    }

    /**
     * 获取魅族权限管理页面的intent
     */
    fun getMEIZUIntent(context: Context): Intent{
       return Intent("com.meizu.safe.security.SHOW_APPSEC").apply {
           putExtra("packageName", context.packageName)
           addCategory(Intent.CATEGORY_DEFAULT)
       }
    }

    /**
     *  获取OPPO权限管理页面的intent
     */
    fun getOPPOIntent(): Intent{
        return Intent().apply {
            component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.PermissionManagerActivity")
        }
    }

}