package com.example.permission.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import kotlin.random.Random

/**
 * 权限检查等工具方法
 * Created by 陈健宇 at 2019/6/3
 */
internal object PermissionUtil {

    /**
     * 判断是否有权限使用Uri
     */
    fun isCanOpenUri(context: Context, uri: Uri): Boolean {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver?.openInputStream(uri)
            false
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            true
        }finally {
            inputStream?.close()
        }
    }

    /**
     * 检查权限是否被授予
     * @return Pair.first是还未被授予的权限，Pair.second是已经被授予的权限，
     */
    fun checkPermissions(context: Context, permissions: List<String>): Pair<MutableList<String>, MutableList<String>>{
        val grantedPermissions = ArrayList<String>()
        val rejectedPermissions = ArrayList<String>()
        for(permission in permissions){
            val isGranted = if(!isSpecialPermission(permission)){
                checkNormalPermission(context, permission)
            }else{
                checkSpecialPermission(context, permission)
            }
            if(isGranted){
                grantedPermissions.add(permission)
            }else{
                rejectedPermissions.add(permission)
            }
        }
        return Pair(rejectedPermissions, grantedPermissions)
    }

    /**
     * 检查权限是否被授予
     */
    fun checkNormalPermission(context: Context, permission: String): Boolean{
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查特殊权限是否被授予
     */
    fun checkSpecialPermission(context: Context, permission: String): Boolean{
        return SpecialUtil.checkPermission(context, permission)
    }

    /**
     * 判断permission是否是特殊权限
     */
    fun isSpecialPermission(permission: String): Boolean{
        return SpecialUtil.getPermissions().contains(permission)
    }

    /**
     * 判断是否需要向用户显示显示UI解释权限申请原因
     */
    fun checkShouldShowRationale(activity: Activity, permission: String): Boolean{
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 随机生成[[initialCode], [maxCode]]之间的数字
     */
    fun generateRandomCode(initialCode: Int = Int.MIN_VALUE, maxCode: Int = Int.MAX_VALUE): Int {
        if(maxCode < initialCode){
            return Random.Default.nextInt()
        }
        return Random.Default.nextInt(maxCode - initialCode + 1) + initialCode
    }

}