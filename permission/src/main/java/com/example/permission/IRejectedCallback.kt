package com.example.permission

import com.example.permission.base.IProcess

/**
 * 权限首次被拒绝时的回调
 * Created by 陈健宇 at 2021/8/12
 */
interface IRejectedCallback {

    /**
     * 当权限首次被拒绝时，该方法回调，可以在该方法中向用户解释申请权限的原因
     * @param process 当征得用户同意时调用相应方法以再次申请被拒绝的权限
     * @param rejectedPermissions 被用户拒绝的权限
     */
    fun onRejected(process: IRejectedProcess, rejectedPermissions: List<String>)

    /**
     * 用于控制是否再次申请权限
     */
    interface IRejectedProcess : IProcess {

        /**
         * 当用户同意再次申请权限时，调用该方法，传入继续申请的权限
         * @param permissions 需要再次申请的权限
         */
        fun requestAgain(permissions: List<String>)

    }

}