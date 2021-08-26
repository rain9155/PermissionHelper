package com.example.permission.proxy

import android.content.Intent
import android.util.SparseArray
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.*
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.utils.forEach
import java.lang.reflect.Constructor
import java.util.concurrent.atomic.AtomicInteger

/**
 * 代理fragment的ViewModel
 * Created by 陈健宇 at 2021/8/25
 */

/**
 * 从[ViewModelProvider]中获取viewModel
 */
fun <T : ViewModel> getViewModel(viewModelStoreOwner: ViewModelStoreOwner, viewModelClazz: Class<T>): T{
    val viewModelProviderClazz: Class<ViewModelProvider> = Class.forName(ViewModelProvider::class.java.name) as Class<ViewModelProvider>
    var oneParamConstructor: Constructor<ViewModelProvider>? = null
    try {
        oneParamConstructor = viewModelProviderClazz.getConstructor(ViewModelStoreOwner::class.java)
    }catch (e: Exception){
        e.printStackTrace()
    }
    if(oneParamConstructor != null){
        return oneParamConstructor.newInstance(viewModelStoreOwner).get(viewModelClazz)
    }
    val twoParamConstructor: Constructor<ViewModelProvider> = viewModelProviderClazz.getConstructor(ViewModelStore::class.java, ViewModelProvider.Factory::class.java)
    return twoParamConstructor.newInstance(viewModelStoreOwner.viewModelStore, ViewModelProvider.NewInstanceFactory()).get(viewModelClazz)
}

/**
 * 代理Fragment的ViewModel公共实现
 */
internal open class ProxyFragmentViewModel : ViewModel() {

    val requestNormalPermissionsResultLiveData = MutableLiveData<PermissionsResult>()
    val requestSpecialPermissionsResultLiveData = MutableLiveData<PermissionsResult>()
    val checkPermissionsResultLiveData = MutableLiveData<PermissionsResult>()

    val permissionResultCallbacks = SparseArray<IPermissionResultsCallback>()
    val waitForCheckPermissions = SparseArray<Array<String>>()
    val waitForCheckSpecialPermissions = SparseArray<SpecialArray>()

    override fun onCleared() {
        super.onCleared()
        permissionResultCallbacks.clear()
        waitForCheckPermissions.clear()
        waitForCheckSpecialPermissions.clear()
    }

    class PermissionsResult(val requestCode: Int, val permissions: Array<String>, val grantResults: BooleanArray)
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