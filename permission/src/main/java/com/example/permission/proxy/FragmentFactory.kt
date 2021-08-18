package com.example.permission.proxy

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.permission.base.IProxyFragment
import com.example.permission.utils.LogUtil
import java.lang.Exception

/**
 * 管理代理Fragment的创建与安装
 * Created by 陈健宇 at 2021/8/18
 */
internal class FragmentFactory(private val fragmentManager: FragmentManager) {

    companion object{
        private const val TAG = "FragmentFactory"
        private const val TAG_FRAGMENT = "ProxyFragment"
    }

    fun installFragment(): IProxyFragment {
        var fragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT)
        if(fragment == null){
            val isInstall = if(tryFindActivityResultRegistry()){
                LogUtil.d(TAG, "installFragment: install ProxyFragmentV2")
                realInstallFragment(ProxyFragmentV2.newInstance())
            }else{
                LogUtil.d(TAG, "installFragment: install ProxyFragmentV1")
                realInstallFragment(ProxyFragmentV1.newInstance())
            }
            if(!isInstall){
                throw InstantiationException("ProxyFragment install fail")
            }
            fragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT)
        }
        return fragment as IProxyFragment
    }

    private fun realInstallFragment(fragment: Fragment): Boolean{
        val transaction = fragmentManager.beginTransaction().add(fragment, TAG_FRAGMENT)
        return try {
            transaction.commitNowAllowingStateLoss()
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    private fun tryFindActivityResultRegistry(): Boolean{
        return try {
            Class.forName("androidx.activity.result.ActivityResultRegistry")
            true
        }catch (e: ClassNotFoundException){
            e.printStackTrace()
            false
        }
    }

}