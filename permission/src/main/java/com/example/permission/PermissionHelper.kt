package com.example.permission

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.permission.base.IProxyFragment
import com.example.permission.base.Request
import com.example.permission.proxy.FragmentFactory
import com.example.permission.proxy.ProxyFragmentV1
import com.example.permission.request.*
import com.example.permission.request.DefaultChain
import com.example.permission.request.PostRequestNode
import com.example.permission.request.PreRequestNode
import com.example.permission.request.RequestNormalNode
import com.example.permission.request.RequestSpecialNode
import kotlin.collections.LinkedHashSet

/**
 * 申请权限帮助类
 * Created by 陈健宇 at 2019/3/25
 */
class PermissionHelper private constructor(private val proxyFragment: IProxyFragment) {

    companion object {
        /**
         * 在Activity中使用
         */
        @JvmStatic
        fun with(activity: FragmentActivity): PermissionHelper{
            return PermissionHelper(FragmentFactory(activity.supportFragmentManager).installFragment())
        }

        /**
         * 在Fragment中使用
         */
        @JvmStatic
        fun with(fragment: Fragment): PermissionHelper{
            return PermissionHelper(FragmentFactory(fragment.childFragmentManager).installFragment())
        }
    }

    private val activity: Activity = proxyFragment.requestActivity()
    private var requestPermissions = LinkedHashSet<String>()
    private var requestCallback: IRequestCallback? = null
    private var rejectedCallback: IRejectedCallback? = null
    private var rejectedForeverCallback: IRejectedForeverCallback? = null
    private var resultCallback: IResultCallback? = null

    /**
     * 以数组形式传入需要请求的权限
     * @param permissions 需要请求的权限
     */
    fun permissions(vararg permissions: String): PermissionHelper{
        requestPermissions.addAll(permissions)
        return this
    }

    /**
     * 以列表形式传入需要请求的权限
     * @param permissions 需要请求的权限
     */
    fun permissions(permissions: List<String>): PermissionHelper{
        requestPermissions.addAll(permissions)
        return this
    }

    /**
     * 在权限请求前向用户解释请求原因
     * @param requestCallback 开始请求权限时的回调
     */
    fun explainBeforeRequest(requestCallback: IRequestCallback): PermissionHelper{
        this.requestCallback = requestCallback
        return this
    }

    /**
     * 在权限首次被拒绝后向用户解释被拒绝权限申请的必要性，征求用户同意请求
     * @param rejectedCallback 权限首次被拒绝时的回调
     */
    fun explainAfterRejected(rejectedCallback: IRejectedCallback): PermissionHelper{
        this.rejectedCallback = rejectedCallback
        return this
    }

    /**
     * 在权限永远被拒绝后向用户解释被拒绝权限申请的必要性，征求用户同意，并引导用户到设置界面开启
     * @param rejectedForeverCallback 权限永远被拒绝时的回调
     */
    fun explainAfterRejectedForever(rejectedForeverCallback: IRejectedForeverCallback): PermissionHelper{
        this.rejectedForeverCallback = rejectedForeverCallback
        return this
    }

    /**
     * 开始申请权限
     * @param resultCallback 权限申请结果回调
     */
    fun request(resultCallback: IResultCallback){
        this.resultCallback = resultCallback
        val originRequest = Request(
            this.activity,
            this.proxyFragment,
            this.requestPermissions.toList(),
            this.requestCallback,
            this.rejectedCallback,
            this.rejectedForeverCallback,
            this.resultCallback
        )
        val interceptors = listOf(
            PreRequestNode(),
            RequestNormalNode(),
            RequestSpecialNode(),
            PostRequestNode(),
            FinishRequestNode()
        )
        DefaultChain(originRequest, interceptors).process(originRequest)
    }
}