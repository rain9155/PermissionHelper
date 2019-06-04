package com.example.permission.callback;

import com.example.permission.bean.Permission;
import com.example.permission.bean.SpecialPermission;

import java.security.SecurityPermission;

/**
 * 特殊权限申请回调接口
 * Created by 陈健宇 at 2019/6/2
 */
public interface ISpecialPermissionCallback {

    /**
     * 用户同意该特殊权限
     * @param permission 特殊权限
     */
    void onAccepted(SpecialPermission permission);

    /**
     * 用户拒绝该特殊权限
     * @param permission 特殊权限
     */
    void onDenied(SpecialPermission permission);
}
