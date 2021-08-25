package com.example.permission.utils

import android.util.SparseArray

/**
 * 扩展某个类添加工具方法
 * Created by 陈健宇 at 2021/8/25
 */

inline fun <T> SparseArray<T>.forEach(action: (T) -> Unit){
    var index = 0
    while (index < size()){
        action.invoke(valueAt(index++))
    }
}

inline fun <T> SparseArray<T>.forEachWithIndex(action: (Int, T) -> Unit){
    var index = 0
    while (index < size()){
        action.invoke(index, valueAt(index))
        ++index
    }
}

fun <T> SparseArray<T>.isNotEmpty(): Boolean{
    return size() > 0
}

fun <T> SparseArray<T>.isEmpty(): Boolean{
    return size() == 0
}