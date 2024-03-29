package com.example.permission.utils

import android.util.SparseArray

/**
 * 扩展某个类添加工具方法
 * Created by 陈健宇 at 2021/8/25
 */


internal inline fun <T> SparseArray<T>.forEachKey(action: (Int) -> Unit) {
    var index = 0
    while (index < size()){
        action.invoke(keyAt(index++))
    }
}

internal inline fun <T> SparseArray<T>.forEachValue(action: (T) -> Unit) {
    var index = 0
    while (index < size()){
        action.invoke(valueAt(index++))
    }
}

internal fun <T> SparseArray<T>.putAll(sparseArray: SparseArray<T>) {
    sparseArray.forEachKey { key ->
        put(key, sparseArray[key])
    }
}

internal fun <T> SparseArray<T>.isNotEmpty(): Boolean {
    return size() > 0
}

internal fun <T> SparseArray<T>?.isEmpty(): Boolean {
    return this == null || !isNotEmpty()
}

internal fun <T> SparseArray<T>.containKey(key: Int): Boolean {
    return indexOfKey(key) >= 0
}

internal fun <T> Array<T>.toStrings(): String {
    if(isEmpty()){
        return "[]"
    }
    val sb = StringBuilder()
    sb.append('[')
    forEachIndexed { index, elememt ->
        sb.append(elememt)
        if(index != size - 1){
            sb.append(", ")
        }
    }
    sb.append("]")
    return sb.toString()
}

internal fun BooleanArray.toStrings(): String {
    if(isEmpty()){
        return "[]"
    }
    val sb = StringBuilder()
    sb.append('[')
    forEachIndexed { index, elememt ->
        sb.append(elememt)
        if(index != size - 1){
            sb.append(", ")
        }
    }
    sb.append("]")
    return sb.toString()
}