package com.example.permission.proxy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.ArrayMap
import android.util.SparseArray
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.IProxyFragment
import com.example.permission.base.PermissionResult
import com.example.permission.utils.LogUtil
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * Created by 陈健宇 at 2021/8/18
 */
internal class ProxyFragmentV2 : Fragment(), IProxyFragment{

    companion object {
        private const val TAG = "ProxyFragmentV2"
        private const val INITIAL_REQUEST_CODE = 0x00000100

        fun newInstance(): ProxyFragmentV2 {
            return ProxyFragmentV2()
        }
    }

    private val normalPermissionsLauncherToCallback = ArrayMap<ActivityResultLauncher<Array<String>>, IPermissionResultsCallback>()
    private lateinit var normalPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var specialPermissionLauncher: ActivityResultLauncher<Intent>
    private val registry = requireActivity().activityResultRegistry
    private val nextLocalRequestCode = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        normalPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val permissionResults = ArrayList<PermissionResult>(results.size)
            results.keys.forEachIndexed{ index, name ->
                permissionResults[index] = PermissionResult(name, results[name]!!)
            }
            normalPermissionsLauncherToCallback[normalPermissionsLauncher]?.onPermissionResults(permissionResults)
        }

        specialPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }
    }

    override fun requestActivity(): Activity {
        return requireActivity()
    }

    override fun requestNormalPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestNormalPermissions(permissions.toTypedArray(), callback)
    }

    override fun requestSpecialPermissions(permissions: List<String>, callback: IPermissionResultsCallback) {
        requestSpecialPermissions(permissions.toTypedArray(), callback)
    }

    override fun gotoSettingsForCheckResults(permissions: List<String>, callback: IPermissionResultsCallback) {
        TODO("Not yet implemented")
    }

    private fun requestNormalPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestNormalPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }
        normalPermissionsLauncherToCallback[normalPermissionsLauncher] = callback
        normalPermissionsLauncher.launch(permissions)
    }

    private fun requestSpecialPermissions(permissions: Array<String>, callback: IPermissionResultsCallback){
        LogUtil.d(TAG, "requestSpecialPermissions: permissions = $permissions")
        if(permissions.isEmpty()){
            callback.onPermissionResults(emptyList())
            return
        }

    }

    private fun <I, O> registerForResults(contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>): ActivityResultLauncher<I> {
        return registry.register(generateKey(), this, contract, callback)
    }

    private fun generateKey(): String{
        return "$TAG#${nextLocalRequestCode.getAndIncrement()}"
    }

}