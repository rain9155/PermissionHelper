package com.example.permissionhelper

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.permission.*
import com.example.permissionhelper.databinding.FragmentPermissionBinding
import java.util.ArrayList

/**
 * 权限申请按钮展示
 */
class PermissionFragment : Fragment() {

    companion object{
        private const val TAG = "PermissionFragment"
    }

    private lateinit var host: Context

    private val permissions = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.REQUEST_INSTALL_PACKAGES,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.PACKAGE_USAGE_STATS,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        host = requireContext()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentPermissionBinding.inflate(inflater)

        binding.btnRequestPermission.setOnClickListener {
            requestPermissions()
        }

        binding.btnExplainBeforeRequest.setOnClickListener {
            requestPermissionsWithExplainBeforeRequest()
        }

        binding.btnExplainAfterRejected.setOnClickListener {
            requestPermissionsWithExplainAfterRejected()
        }

        binding.btnRequestExplain.setOnClickListener {
            requestPermissionsWithExplain()
        }
        return binding.root
    }

    private fun requestPermissions() {
        PermissionHelper.with(this)
            .permissions(permissions)
            .request(object : IResultCallback {
                override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
                    showRequestPermissionsResult(isAllGrant, grantedPermissions, rejectedPermissions)
                }
            })
    }

    private fun requestPermissionsWithExplainBeforeRequest() {
        PermissionHelper.with(this)
            .permissions(permissions)
            .explainBeforeRequest(object : IRequestCallback {
                override fun onRequest(process: IRequestCallback.IRequestProcess, requestPermissions: List<String>) {
                    showAlert(
                        "以下申请的权限是运行时必须的，是否继续申请",
                        getPermissionsLabel(requestPermissions),
                        "否",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "是",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.requestContinue()
                        }
                    )
                }
            }).request(object : IResultCallback {
                override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
                    showRequestPermissionsResult(isAllGrant, grantedPermissions, rejectedPermissions)
                }
            })
    }

    private fun requestPermissionsWithExplainAfterRejected() {
        PermissionHelper.with(this)
            .permissions(permissions)
            .reCallbackAfterConfigurationChanged(false)
            .explainAfterRejected(object : IRejectedCallback {
                override fun onRejected(process: IRejectedCallback.IRejectedProcess, rejectedPermissions: List<String>) {
                    showAlert(
                        "以下点击拒绝的权限是运行时必须的，请申请后再进行后续操作",
                        getPermissionsLabel(rejectedPermissions),
                        "不申请",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "申请",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.requestAgain(rejectedPermissions)
                        }
                    )
                }
            }).explainAfterRejectedForever(object : IRejectedForeverCallback {
                override fun onRejectedForever(process: IRejectedForeverCallback.IRejectedForeverProcess, rejectedForeverPermissions: List<String>) {
                    showAlert(
                        "以下点击永久拒绝的权限是运行时必须的，请前往权限中心同意",
                        getPermissionsLabel(rejectedForeverPermissions),
                        "不前往",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "前往",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.gotoSettings()
                        }
                    )
                }
            }).request(object : IResultCallback {
                override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
                    showRequestPermissionsResult(isAllGrant, grantedPermissions, rejectedPermissions)
                }
            })
    }

    private fun requestPermissionsWithExplain() {
        PermissionHelper.with(this)
            .permissions(permissions)
            .explainBeforeRequest(object : IRequestCallback {
                override fun onRequest(process: IRequestCallback.IRequestProcess, requestPermissions: List<String>) {
                    showAlert(
                        "以下申请的权限是运行时必须的，是否继续申请",
                        getPermissionsLabel(requestPermissions),
                        "否",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "是",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.requestContinue()
                        }
                    )
                }
            }).explainAfterRejected(object : IRejectedCallback {
                override fun onRejected(process: IRejectedCallback.IRejectedProcess, rejectedPermissions: List<String>) {
                    showAlert(
                        "以下点击拒绝的权限是运行时必须的，请申请后再进行后续操作",
                        getPermissionsLabel(rejectedPermissions),
                        "不申请",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "申请",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.requestAgain(rejectedPermissions)
                        }
                    )
                }
            }).explainAfterRejectedForever(object : IRejectedForeverCallback {
                override fun onRejectedForever(process: IRejectedForeverCallback.IRejectedForeverProcess, rejectedForeverPermissions: List<String>) {
                    showAlert(
                        "以下点击永久拒绝的权限是运行时必须的，请前往权限中心同意",
                        getPermissionsLabel(rejectedForeverPermissions),
                        "不前往",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.rejectRequest()
                        },
                        "前往",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            process.gotoSettings()
                        }
                    )
                }
            }).request(object : IResultCallback {
                override fun onResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
                    showRequestPermissionsResult(isAllGrant, grantedPermissions, rejectedPermissions)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun showRequestPermissionsResult(isAllGrant: Boolean, grantedPermissions: List<String>, rejectedPermissions: List<String>) {
        Log.d(TAG, "onPermissionResult: isAllGrant = $isAllGrant, grantedPermissions = $grantedPermissions, rejectedPermissions$rejectedPermissions")
        if (isAllGrant) {
            showAlertWithNotNegative(
                "你同意了所有权限",
                emptyList(),
                "确定",
                null
            )
        } else {
            showAlertWithNotNegative(
                "权限请求结果，以下权限你不同意申请",
                getPermissionsLabel(rejectedPermissions),
                "确定",
                null
            )
        }
    }

    private fun getPermissionsLabel(permissions: List<String>): List<CharSequence> {
        val labels: MutableList<CharSequence> = ArrayList(permissions.size)
        val packageManager = host.packageManager
        for (permission in permissions) {
            try {
                val permissionInfo = packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA)
                labels.add(permissionInfo.loadLabel(packageManager))
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                labels.add("$permission(notFound)")
            }
        }
        return labels
    }

    private fun showAlertWithNotNegative(title: String, items: List<CharSequence>, positive: String, onPositiveClick: DialogInterface.OnClickListener?) {
        showAlert(title, items, "", null, positive, onPositiveClick)
    }

    private fun showAlert(title: String, items: List<CharSequence>, negative: String, onNegativeClick: DialogInterface.OnClickListener?, positive: String, onPositiveClick: DialogInterface.OnClickListener?) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(host)
            .setCancelable(false)
            .setTitle(title)
            .setItems(items.toTypedArray(), null)
        if (!TextUtils.isEmpty(negative)) {
            builder.setNegativeButton(negative) { dialog: DialogInterface, which: Int ->
                onNegativeClick?.onClick(dialog, which)
                dialog.dismiss()
            }
        }
        if (!TextUtils.isEmpty(positive)) {
            builder.setPositiveButton(positive) { dialog: DialogInterface, which: Int ->
                onPositiveClick?.onClick(dialog, which)
                dialog.dismiss()
            }
        }
        builder.create().show()
    }

}