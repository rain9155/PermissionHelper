package com.example.permission

/**
 * 申请流程控制
 * Created by 陈健宇 at 2021/8/12
 */
interface IProcess {

    /**
     * 当用户拒绝继续申请时，调用该方法
     */
    fun rejectRequest()

}