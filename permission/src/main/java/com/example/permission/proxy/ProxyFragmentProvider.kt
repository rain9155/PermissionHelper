package com.example.permission.proxy

import android.util.Log
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.example.permission.base.*
import com.example.permission.base.IProxyFragment
import com.example.permission.base.IProxyFragmentUpdateCallback
import com.example.permission.utils.LogUtil
import java.lang.Exception

/**
 * 管理代理Fragment的创建与安装
 * Created by 陈健宇 at 2021/8/18
 */
internal class ProxyFragmentProvider : IRequestStepCallback.Impl{

    companion object{
        private const val TAG = "FragmentProvider"
        private const val TAG_FRAGMENT = "ProxyFragment"
    }

    @Version
    private val version = VERSION_AUTO
    private var activity: FragmentActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var proxyFragmentAgent: ProxyFragmentAgent
    private val installFragmentLiveData = MutableLiveData<Fragment>()

    /**
     * proxyFragmentUpdateCallback持有宿主activity和代理fragment实例，所以可能会出现内存泄漏，解决方法：
     * 在代理fragment更新时同时更新宿主activity和代理fragment实例，切断和旧的宿主activity和代理fragment实例的引用链
     */
    private val proxyFragmentUpdateCallback = object : IProxyFragmentUpdateCallback {

        override fun onProxyFragmentUpdate(proxyFragment: IProxyFragment) {
            LogUtil.d(TAG, "onProxyFragmentUpdate: proxyFragment = $proxyFragment, provider = ${this@ProxyFragmentProvider}")
            activity = proxyFragment.requestActivity()
            fragmentManager = proxyFragment.requestFragmentManager()
            //宿主销毁重建后重新订阅
            observeInstallFragmentLiveData()
            //代理fragment销毁重建后更新ProxyFragmentAgent
            createOrUpdateProxyFragmentAgent()
        }
    }

    /**
     * 由于观察的是fragment，所以可能会出现内存泄漏，解决办法：
     * 发送完Fragment后要把liveData的data置空，避免data持有fragment实例
     */
    private val installFragmentLiveDataObserver = Observer<Fragment> { fragment ->
        LogUtil.d(TAG, "onChange：fragment = $fragment")
        if(fragmentManager.findFragmentByTag(TAG_FRAGMENT) == null && fragment != null){
            installFragment(fragment)
            installFragmentLiveData.value = null
        }
    }

    constructor(activity: FragmentActivity){
        this.activity = activity
        this.fragmentManager = activity.supportFragmentManager
        observeInstallFragmentLiveData()
        createOrUpdateProxyFragmentAgent()
    }

    constructor(fragment: Fragment){
        this.activity = fragment.requireActivity()
        this.fragmentManager = fragment.childFragmentManager
        observeInstallFragmentLiveData()
        createOrUpdateProxyFragmentAgent()
    }

    fun get() = proxyFragmentAgent

    override fun onRequestStart() {
        super.onRequestStart()
        if(this::proxyFragmentAgent.isInitialized){
            proxyFragmentAgent.obtainFragmentUpdateCallbackManager().add(proxyFragmentUpdateCallback)
        }
    }

    override fun onRequestFinish() {
        super.onRequestFinish()
        if(this::proxyFragmentAgent.isInitialized){
            proxyFragmentAgent.obtainFragmentUpdateCallbackManager().remove(proxyFragmentUpdateCallback)
        }
    }

    private fun observeInstallFragmentLiveData(){
        LogUtil.d(TAG, "observeInstallFragmentLiveData")
        installFragmentLiveData.observe(activity, installFragmentLiveDataObserver)
    }

    private fun createOrUpdateProxyFragmentAgent(){
        var fragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT)
        if(fragment == null){
            fragment = initializeFragment()
            installFragmentLiveData.value = fragment
        }else{
            LogUtil.d(TAG, "proxyFragment already exist")
        }
        val proxyFragment = fragment as IProxyFragment
        if(!this::proxyFragmentAgent.isInitialized){
            proxyFragmentAgent = ProxyFragmentAgent(activity, fragment as IProxyFragment)
        }else {
            LogUtil.d(TAG, "update proxyFragmentAgent")
            proxyFragmentAgent.onProxyFragmentUpdate(proxyFragment)
        }
    }

    private fun initializeFragment(): Fragment{
        val isSelectV2 = if(version == VERSION_V1){
            false
        }else if(version == VERSION_V2){
            true
        }else{
            tryFindActivityResultRegistry()
        }
        return if(isSelectV2){
            LogUtil.d(TAG, "initialize proxyFragmentV2")
            ProxyFragmentV2.newInstance()
        }else{
            LogUtil.d(TAG, "initialize proxyFragmentV1")
            ProxyFragmentV1.newInstance()
        }
    }


    private fun tryFindActivityResultRegistry(): Boolean{
        Log.d(TAG, "tryFindActivityResultRegistry")
        return try {
            Class.forName("androidx.activity.result.ActivityResultRegistry")
            true
        }catch (e: ClassNotFoundException){
            e.printStackTrace()
            false
        }
    }

    private fun installFragment(fragment: Fragment){
        Log.d(TAG, "installFragment: fragment = $fragment")
        val transaction = fragmentManager.beginTransaction().add(fragment, TAG_FRAGMENT)
        try {
            transaction.commitNow()
        } catch (e: Exception) {
            e.printStackTrace()
            throw InstantiationException("$fragment install fail")
        }
    }

    @IntDef(value = [VERSION_AUTO, VERSION_V1, VERSION_V2])
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Version
}