package com.example.permission.proxy

import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.permission.base.IProxyFragment
import com.example.permission.utils.LogUtil

/**
 * 管理代理Fragment的创建与安装
 * Created by 陈健宇 at 2021/8/18
 */
internal class FragmentFactory(private val fragmentManager: FragmentManager) {

    companion object{
        private const val TAG = "FragmentFactory"
        private const val TAG_FRAGMENT = "ProxyFragment"
        private const val VERSION_AUTO = 0x0000
        private const val VERSION_V1 = 0x0001
        private const val VERSION_V2 = 0x0002
    }

    @Version var version = VERSION_AUTO

    fun installFragment(): IProxyFragment {
        var fragment = fragmentManager.findFragmentByTag(TAG_FRAGMENT)
        if(fragment == null){
            val isInstall = if(selectV2()){
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

    private fun selectV2(): Boolean{
        return if(version == VERSION_V1){
            false
        }else if(version == VERSION_V2){
            true
        }else{
            tryFindActivityResultRegistry()
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

    @IntDef(value = [VERSION_AUTO, VERSION_V1, VERSION_V2])
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_SETTER)
    annotation class Version
}