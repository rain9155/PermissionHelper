package com.example.permission

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.permission.base.Request
import com.example.permission.proxy.ProxyFragmentProvider
import com.example.permission.utils.PermissionUtil
import com.example.permission.utils.SettingsUtil
import kotlin.collections.LinkedHashSet

/**
 * 申请权限帮助类
 * Created by 陈健宇 at 2019/3/25
 */
class PermissionHelper private constructor(private val fragmentProvider: ProxyFragmentProvider) {

    companion object {

        /**
         * 使用activity进行初始化
         * @param activity 上下文，activity必须是FragmentActivity
         */
        @JvmStatic
        fun with(activity: FragmentActivity): PermissionHelper {
            return PermissionHelper(ProxyFragmentProvider(activity))
        }

        /**
         * 使用fragment进行初始化
         * @param fragment 上下文，fragment的宿主必须是FragmentActivity
         */
        @JvmStatic
        fun with(fragment: Fragment): PermissionHelper {
            return PermissionHelper(ProxyFragmentProvider(fragment))
        }

        /**
         * 检查权限是否被授予
         * @param context 上下文
         * @param permission 要检查的权限名
         */
        @JvmStatic
        fun checkPermission(context: Context, permission: String): Boolean {
            return PermissionUtil.checkPermission(context, permission)
        }

        /**
         * 跳转到不同厂商的权限设置界面，如果跳转失败(不支持的厂商)，则跳转到应用详情页
         * @param activity 上下文
         * @param requestCode 请求码，如果传递了，须自己在activity中重写[Activity.onActivityResult]
         */
        @JvmStatic
        @JvmOverloads
        fun gotoSettings(activity: Activity, requestCode: Int = 0x9155) {
            activity.startActivityForResult(SettingsUtil.getIntent(activity), requestCode)
        }

    }

    private var requestPermissions = LinkedHashSet<String>()
    private var requestCallback: IRequestCallback? = null
    private var rejectedCallback: IRejectedCallback? = null
    private var rejectedForeverCallback: IRejectedForeverCallback? = null
    private var resultCallback: IResultCallback? = null
    private var reCallbackAfterConfigurationChanged = true

    /**
     * 以数组形式传入需要请求的权限
     * @param permissions 需要请求的权限
     */
    fun permissions(vararg permissions: String): PermissionHelper {
        requestPermissions.addAll(permissions)
        return this
    }

    /**
     * 以列表形式传入需要请求的权限
     * @param permissions 需要请求的权限
     */
    fun permissions(permissions: List<String>): PermissionHelper {
        requestPermissions.addAll(permissions)
        return this
    }

    /**
     * 在权限请求前向用户解释请求原因，当回调发生时，会暂停权限的请求流程，需要调用回调传进来的[IRequestCallback.IRequestProcess]类型参数的相应方法才可以恢复权限的请求流程
     * @param requestCallback 开始请求权限时的回调
     */
    fun explainBeforeRequest(requestCallback: IRequestCallback): PermissionHelper {
        this.requestCallback = requestCallback
        return this
    }

    /**
     * 在权限首次被拒绝后向用户解释被拒绝权限申请的必要性，征求用户同意再次请求，当回调发生时，会暂停权限的请求流程，需要调用回调传进来的[IRejectedCallback.IRejectedProcess]类型参数的相应方法才可以恢复权限的请求流程
     * @param rejectedCallback 权限首次被拒绝时的回调
     */
    fun explainAfterRejected(rejectedCallback: IRejectedCallback): PermissionHelper {
        this.rejectedCallback = rejectedCallback
        return this
    }

    /**
     * 在权限永远被拒绝后向用户解释被拒绝权限申请的必要性，征求用户同意，并引导用户到设置界面开启, 当回调发生时，会暂停权限的请求流程，需要调用回调传进来的[IRejectedForeverCallback.IRejectedForeverProcess]类型参数的相应方法才可以恢复权限的请求流程
     * @param rejectedForeverCallback 权限永远被拒绝时的回调
     */
    fun explainAfterRejectedForever(rejectedForeverCallback: IRejectedForeverCallback): PermissionHelper {
        this.rejectedForeverCallback = rejectedForeverCallback
        return this
    }

    /**
     * PermissionHelper在系统配置变更后(例如屏幕旋转)也可以恢复之前的权限请求流程，如果你设置了[explainBeforeRequest]、[explainAfterRejected]或[explainAfterRejectedForever]回调，需要你在回调发生时调用对应[IProcess]的方法才可以继续权限请求流程，
     * 如果当回调发生时恰好发生系统配置变更，那么回调中与用户交互的部分就会丢失，例如你在回调中弹出了一个弹窗向用户解释权限申请原因，需要用户点击弹窗的确定或取消按钮才会继续调用[IProcess]的相应方法，那么当系统配置发生变更后，弹窗就会消失，这时用户就
     * 没法点击弹窗相应按钮，就会由于没有调用[IProcess]的相应方法中断权限的申请流程，所以PermissionHelper针对这种情况，支持当系统配置变更后再次回调相应的回调，从而恢复权限申请流程，如果不需要，可以设置[reCallback]为false，默认为true
     * @param reCallback true表示支持当系统配置变更后再次回调[explainBeforeRequest]、[explainAfterRejected]或[explainAfterRejectedForever]回调
     */
    fun reCallbackAfterConfigurationChanged(reCallback: Boolean): PermissionHelper {
        this.reCallbackAfterConfigurationChanged = reCallback
        return this
    }

    /**
     * 开始申请权限
     * @param resultCallback 权限申请结果回调
     */
    fun request(resultCallback: IResultCallback) {
        this.resultCallback = resultCallback
        requestInternal()
    }

    private fun requestInternal() {
        fragmentProvider.get().obtainRequestManager().startRequest(
            Request(
                fragmentProvider,
                reCallbackAfterConfigurationChanged,
                requestCallback,
                rejectedCallback,
                rejectedForeverCallback,
                resultCallback,
                requestPermissions.toMutableList()
            )
        )
    }
}