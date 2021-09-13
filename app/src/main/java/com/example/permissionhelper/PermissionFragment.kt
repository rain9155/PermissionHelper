package com.example.permissionhelper

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.permissionhelper.databinding.FragmentPermissionBinding

/**
 * 权限申请按钮展示
 */
class PermissionFragment : Fragment() {

    companion object{
        private const val TAG = "PermissionFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentPermissionBinding.inflate(inflater)
        return binding.root
    }

}