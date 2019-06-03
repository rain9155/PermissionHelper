package com.example.permission.callback;

import com.example.permission.bean.Permission;
import com.example.permission.bean.SpecialPermission;

import java.security.SecurityPermission;

/**
 * 特殊权限申请回调接口
 * Created by 陈健宇 at 2019/6/2
 */
public interface ISpecialPermissionCallback {

    void onAccepted(SpecialPermission permission);//setResult(OK)

    void onDenied(SpecialPermission permission);//setResult(CANCEL)
}
