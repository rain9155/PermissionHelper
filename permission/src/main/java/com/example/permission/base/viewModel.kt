package com.example.permission.base

import android.util.SparseArray
import androidx.lifecycle.*
import com.example.permission.utils.LogUtil
import java.lang.reflect.Constructor
import com.example.permission.proxy.*

/**
 * 代理fragment的ViewModel
 * Created by 陈健宇 at 2021/8/25
 */

private const val TAG = "ViewModel"

/**
 * 从[ViewModelProvider]中获取viewModel
 */
internal fun <T : ViewModel> getViewModel(viewModelStoreOwner: ViewModelStoreOwner, viewModelClazz: Class<T>): T{
    val viewModelProviderClazz: Class<ViewModelProvider> = Class.forName(ViewModelProvider::class.java.name) as Class<ViewModelProvider>
    var oneParamConstructor: Constructor<ViewModelProvider>? = null
    try {
        oneParamConstructor = viewModelProviderClazz.getConstructor(ViewModelStoreOwner::class.java)
    }catch (e: Exception){
        LogUtil.e(TAG, "getViewModel: e = $e")
    }
    if(oneParamConstructor != null){
        return oneParamConstructor.newInstance(viewModelStoreOwner).get(viewModelClazz)
    }
    val twoParamConstructor: Constructor<ViewModelProvider> = viewModelProviderClazz.getConstructor(ViewModelStore::class.java, ViewModelProvider.Factory::class.java)
    return twoParamConstructor.newInstance(viewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory()).get(viewModelClazz)
}


/**
 * 权限请求结果暂存
 */
internal class PermissionsResult(val requestCode: Int, val permissions: Array<String>, val grantResults: BooleanArray)

/**
 * 封装特殊权限集合操作
 */
internal class SpecialArray(private val permissions: Array<String>) {

    private var index = 0
    private var grantResults = BooleanArray(permissions.size)

    fun size(): Int {
        return permissions.size
    }

    fun hasNext(): Boolean {
        return index < permissions.size
    }

    fun nextPermission(): String {
        if (!hasNext()) {
            return ""
        }
        return permissions[index++]
    }

    fun hasPrior(): Boolean {
        return index > 0
    }

    fun priorPermission(): String {
        if (!hasPrior()) {
            return ""
        }
        return permissions[index - 1]
    }

    fun get(index: Int): String {
        if (index < 0 || index >= size()) {
            return ""
        }
        return permissions[index]
    }

    fun getPermissions(): Array<String> {
        return permissions.clone()
    }

    fun getGrantResults(): BooleanArray {
        return grantResults.clone()
    }

    fun setPriorGrantResult(granted: Boolean) {
        if (!hasPrior()) {
            return
        }
        grantResults[index - 1] = granted
    }
}

/**
 * 代理Fragment的ViewModel公共实现
 */
internal open class ProxyFragmentViewModel : ViewModel() {

    val requestNormalPermissionsResultLiveData = MutableLiveData<PermissionsResult>()
    val requestSpecialPermissionsResultLiveData = MutableLiveData<PermissionsResult>()
    val checkPermissionsResultLiveData = MutableLiveData<PermissionsResult>()

    val waitForCheckPermissions = SparseArray<Array<String>>()
    val waitForCheckSpecialPermissions = SparseArray<SpecialArray>()

    val permissionResultCallbacks = SparseArray<IPermissionResultsCallback>()
    val proxyFragmentUpdateCallbacks = ArrayList<IProxyFragmentUpdateCallback>()

    var requestManager: IRequestManager? = null

    override fun onCleared() {
        super.onCleared()
        waitForCheckPermissions.clear()
        waitForCheckSpecialPermissions.clear()
        permissionResultCallbacks.clear()
        proxyFragmentUpdateCallbacks.clear()
        requestManager?.clearRequests()
    }
}

/**
 * [ProxyFragmentV1]的ViewModel
 */
internal class ProxyFragmentV1ViewModel : ProxyFragmentViewModel()

/**
 * [ProxyFragmentV2]的ViewModel
 */
internal class ProxyFragmentV2ViewModel : ProxyFragmentViewModel() {

    val requestNormalPermissionsLaunchedKeys = ArrayList<Int>()
    val requestSpecialPermissionsLaunchedKeys = ArrayList<Int>()
    val checkPermissionsLaunchedKeys = ArrayList<Int>()

    override fun onCleared() {
        super.onCleared()
        requestNormalPermissionsLaunchedKeys.clear()
        requestSpecialPermissionsLaunchedKeys.clear()
        checkPermissionsLaunchedKeys.clear()
    }

}