package com.example.permission.proxy

/**
 * 封装特殊权限集合操作
 * @author chenjianyu
 * @date 2021/8/22
 */
internal class SpecialArray(private val permissions: Array<String>) {

    private var index = 0
    private val grantResults = BooleanArray(permissions.size)

    fun size(): Int{
        return permissions.size
    }

    fun hasNext(): Boolean{
        return index < permissions.size
    }

    fun nextPermission(): String{
        if(!hasNext()){
            return ""
        }
        return permissions[index++]
    }

    fun hasPrior(): Boolean{
        return index > 0
    }

    fun priorPermission(): String{
        if(!hasPrior()){
            return ""
        }
        return permissions[index - 1]
    }

    fun get(index: Int): String{
        if(index < 0 || index >= size()){
            return ""
        }
        return permissions[index]
    }

    fun getPermissions(): Array<String>{
        return permissions.clone()
    }

    fun getGrantResults(): BooleanArray{
        return grantResults.clone()
    }

    fun setPriorGrantResult(granted: Boolean){
        if(!hasPrior()){
            return
        }
        grantResults[index - 1] = granted
    }

}