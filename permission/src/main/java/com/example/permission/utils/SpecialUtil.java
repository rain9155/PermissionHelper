package com.example.permission.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.example.permission.BuildConfig;
import com.example.permission.bean.Permission;
import com.example.permission.bean.SpecialPermission;

import java.util.ArrayList;
import java.util.List;

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
     * 检查特殊权限，返回对应的权限授权值
     * @param special 特殊权限
     * @param context 上下文
     * @return true表示用户同意授权，false表示用户拒绝授权
     */
    public static boolean checkSpecialPermissions(SpecialPermission special, Context context) {
        boolean isGranted = false;
        switch (special){
            case INSTALL_UNKNOWN_APP:
                isGranted = checkSpecialInstallUnkownApp(context);
                break;
            default:
                break;
        }
        return isGranted;
    }

}
