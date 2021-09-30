package com.example.permissionhelper

import android.Manifest
import android.view.LayoutInflater
import com.example.permission.IRequestCallback.IRequestProcess
import android.content.DialogInterface
import com.example.permission.IRejectedCallback.IRejectedProcess
import com.example.permission.IRejectedForeverCallback.IRejectedForeverProcess
import android.content.Intent
import android.text.TextUtils
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.permission.*
import com.example.permissionhelper.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(LayoutInflater.from(this)).root)
        Log.d(TAG, "onCreate: orientation = $requestedOrientation, lastNonConfigurationInstance = $lastCustomNonConfigurationInstance, name = ${this::class.java.name}")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_start_permission_activity) {
            startActivity(Intent(this, PermissionGroupActivity::class.java))
        } else if (item.itemId == R.id.item_start_permission_settings) {
            PermissionHelper.gotoSettings(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        super.onRetainCustomNonConfigurationInstance()
        Log.d(TAG, "onRetainCustomNonConfigurationInstance")
        return "testData"
    }

}