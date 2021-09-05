package com.example.permission

/**
 * 权限开始申请时的回调
 * Created by 陈健宇 at 2021/8/12
 */
interface IRequestCallback{

    /**
     * 当权限开始申请时，该方法回调，可以在该方法中向用户解释申请权限的原因
     * @param process 当征得用户同意时调用相应方法以继续权限申请流程
     * @param requestPermissions 即将要申请的权限
     */
    fun onRequest(process: IRequestProcess, requestPermissions: List<String>)

    /**
     * 用于控制是否继续权限申请
     */
    interface IRequestProcess : IProcess {

        /**
         * 当继续权限申请时，调用该方法
         */
        fun requestContinue()

    }

}