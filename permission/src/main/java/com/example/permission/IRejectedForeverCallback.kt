package com.example.permission

import com.example.permission.base.IProcess

/**
 * 权限永远被拒绝时的回调
 * Created by 陈健宇 at 2021/8/12
 */
interface IRejectedForeverCallback {

    /**
     * 当权限永远被拒绝时，该方法回调，可以在该方法中向用户解释申请权限的原因
     * @param process 当征得用户同意时调用相应方法以引导用户去设置同意你的权限申请
     * @param rejectedForeverPermissions 被用户永远拒绝的权限
     */
    fun onRejectedForever(process: IRejectedForeverProcess, rejectedForeverPermissions: List<String>)

    /**
     * 用于控制是否跳转到设置界面，继续权限申请
     */
    interface IRejectedForeverProcess : IProcess {

        /**
         * 当引导用户去设置界面同意你的权限申请时，调用该方法
         */
        fun gotoSettings()

    }

}