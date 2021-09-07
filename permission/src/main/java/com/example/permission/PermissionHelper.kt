package com.example.permission

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.permission.base.Request
import com.example.permission.proxy.ProxyFragmentProvider
import com.example.permission.request.*
import com.example.permission.request.DefaultChain
import com.example.permission.request.PostRequestNode
import com.example.permission.request.PreRequestNode
import com.example.permission.request.RequestNormalNode
import com.example.permission.request.RequestSpecialNode
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SettingsUtil
import kotlin.collections.LinkedHashSet

/**
 * 申请权限帮助类
 * Created by 陈健宇 at 2019/3/25
 */
class PermissionHelper private constructor(private val proxyFragmentProvider: ProxyFragmentProvider) {

    companion object {

        /**
         * 使用activity进行初始化
         * @param activity 上下文，activity必须是FragmentActivity
         */
        @JvmStatic
        fun with(activity: FragmentActivity): PermissionHelper{
            return PermissionHelper(ProxyFragmentProvider(activity))
        }

        /**
         * 使用fragment进行初始化
         * @param fragment 上下文，fragment的宿主必须是FragmentActivity
         */
        @JvmStatic
        fun with(fragment: Fragment): PermissionHelper{
            return PermissionHelper(ProxyFragmentProvider(fragment))
        }

        /**
         * 检查权限是否被授予
         * @param context 上下文
         * @param permission 要检查的权限名
         */
        @JvmStatic
        fun checkPermission(context: Context, permission: String): Boolean{
            return PermissionUtil.checkPermission(context, permission)
        }

        /**
         * 跳转到不同厂商的权限设置界面，如果跳转失败(不支持的厂商)，则跳转到应用详情页
         * @param activity 上下文
         * @param requestCode 请求码，如果传递了，须自己在activity中重写[Activity.onActivityResult]
         */
        @JvmStatic
        @JvmOverloads
        fun gotoSettings(activity: Activity, requestCode: Int = 0x9155){
            activity.startActivityForResult(SettingsUtil.getIntent(activity), requestCode)
        }

    }

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
     * 在权限首次被拒绝后向用户解释被拒绝权限申请的必要性，征求用户同意再次请求
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
            this.proxyFragmentProvider,
            this.requestCallback,
            this.rejectedCallback,
            this.rejectedForeverCallback,
            this.resultCallback,
            this.requestPermissions.toMutableList(),
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