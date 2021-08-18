package com.example.permission.proxy

import android.app.Activity
import androidx.fragment.app.Fragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.IProxyFragment

/**
 *
 * Created by 陈健宇 at 2021/8/18
 */
internal class ProxyFragmentV2 : Fragment(), IProxyFragment{

    companion object {
        private const val TAG = "ProxyFragmentV2"

        fun newInstance(): ProxyFragmentV2 {
            return ProxyFragmentV2()
        }
    }

    override fun requestActivity(): Activity {
        return requireActivity()
    }

    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        TODO("Not yet implemented")
    }

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        TODO("Not yet implemented")
    }

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) {
        TODO("Not yet implemented")
    }
}