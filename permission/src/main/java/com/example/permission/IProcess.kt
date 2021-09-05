package com.example.permission

/**
 * 申请流程控制
 * Created by 陈健宇 at 2021/8/12
 */
interface IProcess {

    /**
     * 当终止申请流程时，调用该方法
     */
    fun requestTermination()

}