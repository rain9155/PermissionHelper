package com.example.permission

/**
 * 权限申请结果回调
 * Created by 陈健宇 at 2021/8/13
 */
interface IResultCallback {

    /**
     * 权限申请成功或失败时，该方法回调
     * @param isAllGrant 所有请求的权限是否都申请成功
     * @param grantedPermissions 申请成功的权限，有三种情况：
     *                            1、用户之前已经授权了此权限
     *                            2、用户在授权弹窗中点击授权此权限
     *                            3、系统版本小于android 6.0
     * @param rejectedPermissions 申请失败的权限, 有两种情况：
     *                            1、用户在授权弹窗点击拒绝授权此权限
     *                            2、用户在授权弹窗点击拒绝授权此权限，并勾选了don’t ask again(android 11之后用户连续两次点击拒绝授权等同于don’t ask again)
     */
    fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>)

}