package com.example.permission.base

import android.app.Activity
import android.content.Context
import com.example.permission.IRejectedCallback
import com.example.permission.IRejectedForeverCallback
import com.example.permission.IRequestCallback
import com.example.permission.IResultCallback

/**
 * 权限请求过程使用责任链模式，这里定义了责任链的的输入、连接、节点
 * Created by 陈健宇 at 2021/8/15
 */

/**
 * 输入
 */
internal data class Request(
    val activity: Activity,
    val proxyFragment: IProxyFragment,
    var requestPermissions: MutableList<String>,
    var requestCallback: IRequestCallback?,
    var rejectedCallback: IRejectedCallback?,
    var rejectedForeverCallback: IRejectedForeverCallback?,
    var resultCallback: IResultCallback?,
    var grantedPermissions: MutableList<String> = ArrayList(),
    var rejectedPermissions: MutableList<String> = ArrayList()
)


/**
 * 连接
 */
internal interface IChain {

    fun getRequest(): Request

    fun process(request: Request, finish: Boolean = false, restart: Boolean = false, again: Boolean = false)

}

/**
 * 节点
 */
internal interface INode {

    fun handle(chain: IChain)

}