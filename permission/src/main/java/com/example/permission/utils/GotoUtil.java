package com.example.permission.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.permission.BuildConfig;

/**
 * 跳转到各厂商的权限界面工具类
 * Created by 陈健宇 at 2019/6/3
 */
public class GotoUtil {

    /**
     * 跳转到应用详情界面
     * @param requestCode 请求码
     */
    public static void gotoAppDetail(Activity context, int requestCode) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivityForResult(intent, requestCode);
    }

    /**
     * 华为的权限管理页面
     * @param requestCode 请求码
     */
    public static void gotoHuaweiPermission(Activity context, int requestCode) {
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");//华为权限管理
            intent.setComponent(comp);
            context.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
            gotoAppDetail(context, requestCode);
        }
    }

    /**
     * MIUI的权限管理页面
     * @param requestCode 请求码
     */
    public static void gotoMiuiPermission(Activity context, int requestCode) {
        try {
            // MIUI 8
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                // MIUI 5/6/7
                Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
                intent.putExtra("extra_pkgname", context.getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivityForResult(intent, requestCode);
            } catch (Exception e1) {
                e1.printStackTrace();
                gotoAppDetail(context, requestCode);
            }
        }
    }

    /**
     * 跳转到魅族的权限管理系统
     */
    public static void gotoMeizuPermission(Activity context, int requestCode) {
        try {
            Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("packageName", BuildConfig.APPLICATION_ID);
            context.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
            gotoAppDetail(context, requestCode);
        }
    }

}
