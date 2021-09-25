package com.example.permission.request

import android.Manifest
import android.text.TextUtils
import com.example.permission.base.*
import com.example.permission.base.IChain
import com.example.permission.base.INode
import com.example.permission.base.IPermissionResultsCallback
import com.example.permission.base.PERMISSION_LOCATION_PROVIDER
import com.example.permission.base.PermissionResult
import com.example.permission.utils.PermissionUtil

/**
 * 权限请求，请求[Manifest.permission_group.LOCATION]权限组的权限
 * Created by 陈健宇 at 2021/9/25
 */
internal class RequestLocationNode : INode {

    companion object{
        private const val TAG = "RequestLocationNode"
    }

    private val locationGroup = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )
    //后台定位权限独立申请
    private var backgroundLocationPermission: String? = null

    override fun handle(chain: IChain) {
        val request = chain.getRequest()

        val locationPermissions = request.requestPermissions.filter { permission ->
            locationGroup.contains(permission)
        }.toMutableList()

        if(locationPermissions.isNotEmpty()) {
            backgroundLocationPermission = locationPermissions.find { permission ->
                Manifest.permission.ACCESS_BACKGROUND_LOCATION == permission
            }
            locationPermissions.remove(backgroundLocationPermission)
            requestLocationPermissions(chain, request, locationPermissions)
        }else {
            chain.process(request)
        }
    }

    private fun requestLocationPermissions(chain: IChain, request: Request, locationPermissions: List<String>) {
        //先检查定位开关有没有打开，如果没有打开就前往设置页面打开
        if(PermissionUtil.checkPermission(request.getActivity(), PERMISSION_LOCATION_PROVIDER)) {
            realRequestLocationPermissions(chain, request, locationPermissions, true)
        }else {
            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPermissions(request, listOf(PERMISSION_LOCATION_PROVIDER))
            }
            request.getProxyFragment().requestSpecialPermissions(listOf(PERMISSION_LOCATION_PROVIDER), object : IPermissionResultsCallback {
                override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                    permissionResults.forEach { result ->
                        realRequestLocationPermissions(chain, request, locationPermissions, result.granted)
                    }
                }
            })
        }
    }

    private fun realRequestLocationPermissions(chain: IChain, request: Request, locationPermissions: List<String>, locationProviderEnable: Boolean) {
        if(locationProviderEnable) {
            request.getRequestStepCallbackManager().dispatchRequestStep { callback ->
                callback.onRequestPermissions(request, locationPermissions)
            }
            request.getProxyFragment().requestNormalPermissions(locationPermissions, object : IPermissionResultsCallback {
                override fun onPermissionResults(permissionResults: List<PermissionResult>) {
                    request.divisionRequestPermissionsByPermissionResults(permissionResults)
                    processNextOrRequestAgain(chain, request, locationProviderEnable)
                }
            })
        }else {
            request.requestPermissions.removeAll(locationPermissions)
            request.rejectedPermissions.addAll(locationPermissions)
            processNextOrRequestAgain(chain, request, locationProviderEnable)
        }
    }

    private fun processNextOrRequestAgain(chain: IChain, request: Request, locationProviderEnable: Boolean) {
        if (!TextUtils.isEmpty(backgroundLocationPermission)) {
            val locationPermissions = listOf(backgroundLocationPermission!!).apply {
                backgroundLocationPermission = null
            }
            realRequestLocationPermissions(chain, request, locationPermissions, locationProviderEnable)
        } else {
            chain.process(request)
        }
    }

}