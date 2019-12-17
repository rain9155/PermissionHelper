package com.example.permission;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;


import com.example.permission.bean.Permission;
import com.example.permission.bean.SpecialPermission;
import com.example.permission.callback.IPermissionCallback;
import com.example.permission.callback.IPermissionsCallback;
import com.example.permission.callback.ISpecialPermissionCallback;
import com.example.permission.proxy.PermissionFragment;
import com.example.permission.utils.CommonUtil;
import com.example.permission.utils.GotoUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.example.permission.utils.SpecialUtil.*;
import static com.example.permission.utils.GotoUtil.*;

/**
 * 申请权限帮助类
 * Created by 陈健宇 at 2019/3/25
 */
public class PermissionHelper {

    private static final String TAG_PERMISSION_FRAGMENT = PermissionFragment.class.getName();
    private Activity mActivity;
    private static PermissionHelper sinstance = null;

    private PermissionHelper(){}

    public static PermissionHelper getInstance(){
        if(sinstance == null){
            synchronized (PermissionHelper.class){
                PermissionHelper permissionHelper;
                if(sinstance == null){
                    permissionHelper = new PermissionHelper();
                    sinstance = permissionHelper;
                }
            }
            sinstance = new PermissionHelper();
        }
        return sinstance;
    }

    /**
     * 必须传入的Activity
     */
    public PermissionHelper with(Activity activity){
        this.mActivity = activity;
        return this;
    }

    /**
     * 可以再基类中统一传入Activity，就无需每次调用with方法
     */
    public void init(Activity activity){
        this.mActivity = activity;
    }

    /**
     * 请求单个权限
     * @param name 权限名
     * @param callback 权限申请回调
     */
    @SuppressLint("NewApi")
    public void requestPermission(@NonNull String name, IPermissionCallback callback){
        String[] requestPermissions = checkPermissions(new String[]{name});
        if(requestPermissions.length == 0){
            callback.onAccepted(new Permission(name));
            return;
        }
        getPermissionFragment(mActivity).requestPermissions(
                requestPermissions,
                (permissionsResult) -> handlePermissionResult(callback, permissionsResult[0])
        );
    }

    /**
     * 请求多个权限
     * @param permissions 权限列表
     * @param callback 权限申请回调
     */
    @SuppressLint("NewApi")
    public void requestPermissions(@NonNull String[] permissions, @NonNull IPermissionsCallback callback){
        String[] requestPermissions = checkPermissions(permissions);
        if(requestPermissions.length == 0){
            callback.onAccepted(CommonUtil.toList(permissions));
            return;
        }
        getPermissionFragment(mActivity).requestPermissions(
                requestPermissions,
                (permissionsResult) -> handlePermissionsResult(callback, permissionsResult)
        );
    }

    @SuppressLint("NewApi")
    public void requestSpecialPermission(@NonNull SpecialPermission specialPermission, @NonNull ISpecialPermissionCallback callback){
        if(checkSpecialPermission(specialPermission)){
            callback.onAccepted(specialPermission);
            return;
        }
        getPermissionFragment(mActivity).requestSpecialPermission(
                specialPermission,
                (permissionsResult) -> handleSpecialPermissionResult(permissionsResult[0], callback)
        );
    }


    /**
     * 检查单个权限是否申请过
     * @param name 要检查的权限名
     * @return true表示申请过，false反之
     */
    public boolean checkPermission(String name){
        verfiy();
        return checkPermissions(new String[]{name}).length == 0;
    }

    /**
     * 检查多个权限是否申请过
     * @param permissions 多个权限数组
     * @return 返回还没有被授权同意的权限数组，如果数组.length==0, 说明permissions都被授权同意了
     */
    public String[] checkPermissions(@NonNull String[] permissions){
        verfiy();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return new String[]{};
        List<Permission> rejectedPermissions = new ArrayList<>();
        for(String permission : permissions){
            if(ContextCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED){
                rejectedPermissions.add(new Permission(permission));
            }
        }
        return CommonUtil.toArray(rejectedPermissions);
    }

    /**
     * 检查特殊权限是否申请过
     * @param special 要检查的特殊权限
     * @return true表示申请过，false反之
     */
    public boolean checkSpecialPermission(SpecialPermission special){
        verfiy();
        return checkSpecialPermissions(special, mActivity);
    }

    /**
     * 跳转到不同厂商的Permission设置界面，不满足条件，默认跳到应用详情界面
     */
    public void gotoPermissionDetail(){
        verfiy();
        gotoPermissionDetail(0x9155);
    }

    /**
     * 跳转到不同厂商的Permission设置界面，不满足条件，默认跳到应用详情界面
     * 可以带请求码跳转，需要自行重写OnActivityResult（）
     * 在OnActivityResult中根据请求码再次检查之前被用户拒绝的权限，看用户有没有打开该权限
     * @param requestCode 请求码
     */
    public void gotoPermissionDetail(int requestCode){
        verfiy();
        String brand = Build.BRAND;
        if (TextUtils.equals(brand.toLowerCase(), "redmi") || TextUtils.equals(brand.toLowerCase(), "xiaomi")) {
            gotoMiuiPermission(mActivity, requestCode);
        } else if (TextUtils.equals(brand.toLowerCase(), "meizu")) {
            gotoMeizuPermission(mActivity, requestCode);
        } else if (TextUtils.equals(brand.toLowerCase(), "huawei") || TextUtils.equals(brand.toLowerCase(), "honor")) {
            gotoHuaweiPermission(mActivity, requestCode);
        } else {
            GotoUtil.gotoAppDetail(mActivity, requestCode);
        }
    }

    /**
     * 跳转到应用详情页
     */
    public void gotoAppDetail(){
        verfiy();
        gotoAppDetail(0x9156);
    }

    /**
     * 跳转到应用详情页，带请求码
     * @param requestCode 请求码
     */
    public void gotoAppDetail(int requestCode){
        verfiy();
        GotoUtil.gotoAppDetail(mActivity, requestCode);
    }

    private void handlePermissionResult(IPermissionCallback callback, Permission permission) {
        if (permission.granted) {
            callback.onAccepted(permission);
        } else {
            if (permission.shouldShowRequestPermissionRationable) {
                callback.onDenied(permission);
            } else {
                callback.onDeniedAndReject(permission);
            }
        }
    }

    private void handlePermissionsResult(@NonNull IPermissionsCallback callback, Permission[] permissionsResult) {
        List<Permission> grantedPermissions = new ArrayList<>();
        List<Permission> deniedPermissions = new ArrayList<>();
        List<Permission> deniedAndRejectedAndPermissions = new ArrayList<>();
        for(int i = 0; i < permissionsResult.length; i++){
            Permission permission = permissionsResult[i];
            if(permission.granted){
                grantedPermissions.add(permission);
            }else {
                deniedPermissions.add(permission);
                if(!permission.shouldShowRequestPermissionRationable){
                    deniedAndRejectedAndPermissions.add(permission);
                }
            }
        }
        if(!grantedPermissions.isEmpty()) callback.onAccepted(grantedPermissions);
        if(!deniedPermissions.isEmpty()) callback.onDenied(deniedPermissions);
        if(!deniedAndRejectedAndPermissions.isEmpty()) callback.onDeniedAndReject(deniedPermissions, deniedAndRejectedAndPermissions);
    }

    private void handleSpecialPermissionResult(Permission permission, ISpecialPermissionCallback callback) {
        if(permission.granted){
            callback.onAccepted(permission.specialPermission);
        }else {
            callback.onDenied(permission.specialPermission);
        }
    }

    private PermissionFragment getPermissionFragment(Activity activity){
        verfiy();
        if(!(activity instanceof FragmentActivity)) throw new IllegalArgumentException("The argument passed must be FragmentActivity or it's sub class");
        FragmentManager manager = ((FragmentActivity)activity).getSupportFragmentManager();
        PermissionFragment fragment = (PermissionFragment) manager.findFragmentByTag(TAG_PERMISSION_FRAGMENT);
        if(fragment == null){
            fragment = PermissionFragment.newInstance();
            FragmentTransaction transaction = manager.beginTransaction();
                    transaction.add(fragment, TAG_PERMISSION_FRAGMENT)
                    .commitAllowingStateLoss();
            try{
                manager.executePendingTransactions();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
        return fragment;
    }

    private void verfiy() {
        if(mActivity == null) throw new NullPointerException("The Activity is null, must call with(Activity acitity) after getInstance()!");
    }

    @Deprecated
    public static boolean isUriRequiresPermissions(@NonNull Context context, @NonNull Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            if (stream != null) {
                stream.close();
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
