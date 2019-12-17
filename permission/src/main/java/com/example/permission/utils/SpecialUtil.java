package com.example.permission.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.example.permission.bean.SpecialPermission;

/**
 * 检查各种特殊权限或创建各种特殊Intent的工具类
 * Created by 陈健宇 at 2019/6/2
 */
public class SpecialUtil {

    /**
     * 检查特殊权限 - 安装未知来源应用
     * @return true表示用户同意授权，false表示用户拒绝授权
     */
    public static boolean checkSpecialInstallUnkownApp(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        return context.getPackageManager().canRequestPackageInstalls();
    }

    /**
     * 检查特殊权限 - 修改系统设置
     * @return true表示用户同意授权，false表示用户拒绝授权
     */
    public static boolean checkSpecialWriteSystemSettings(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.System.canWrite(context);
    }

    /**
     * 检查特殊权限 - 悬浮窗权限
     * @return true表示用户同意授权，false表示用户拒绝授权
     */
    public static boolean checkSpecialSystemAlertWindow(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.canDrawOverlays(context);
    }

}
