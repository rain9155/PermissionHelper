package com.example.permissionhelper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.permission.PermissionHelper;
import com.example.permission.bean.SpecialPermission;
import com.example.permission.callback.ISpecialPermissionCallback;

public class SpecialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special);

        //统一初始化，后面不用使用with(this)
        PermissionHelper.getInstance().init(this);

        //install_unkown_app
        findViewById(R.id.btn_check_install_app).setOnClickListener(v -> {
            if(PermissionHelper.getInstance().checkSpecialPermission(SpecialPermission.INSTALL_UNKNOWN_APP)){
                Toast.makeText(this, "你已经允许该应用安装未知来源应用了", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "该应用还没有被允许安装未知来源应用", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_get_install_app).setOnClickListener(v -> {
            PermissionHelper.getInstance().requestSpecialPermission(
                    SpecialPermission.INSTALL_UNKNOWN_APP,
                    new ISpecialPermissionCallback() {
                        @Override
                        public void onAccepted(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你同意该应用安装未知来源应用", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDenied(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你拒绝该应用安装未知来源应用", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        //write_system_settings
        findViewById(R.id.btn_check_write_settins).setOnClickListener(v -> {
            if(PermissionHelper.getInstance().checkSpecialPermission(SpecialPermission.WRITE_SYSTEM_SETTINGS)){
                Toast.makeText(this, "你已经允许该应用修改系统设置了", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "该应用还没有被允许修改系统设置", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_get_wirte_settings).setOnClickListener(v -> {
            PermissionHelper.getInstance().requestSpecialPermission(
                    SpecialPermission.WRITE_SYSTEM_SETTINGS,
                    new ISpecialPermissionCallback() {
                        @Override
                        public void onAccepted(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你同意该应用修改系统设置", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDenied(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你拒绝该应用修改系统设置", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        //system_alert_window
        findViewById(R.id.btn_check_system_window).setOnClickListener(v -> {
            if(PermissionHelper.getInstance().checkSpecialPermission(SpecialPermission.SYSTEM_ALERT_WINDOW)){
                Toast.makeText(this, "你已经允许该应用设置悬浮窗了", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "该应用还没有被允许设置悬浮窗", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_get_system_window).setOnClickListener(v -> {
            PermissionHelper.getInstance().requestSpecialPermission(
                    SpecialPermission.SYSTEM_ALERT_WINDOW,
                    new ISpecialPermissionCallback() {
                        @Override
                        public void onAccepted(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你同意该应用设置悬浮窗", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDenied(SpecialPermission permission) {
                            Toast.makeText(SpecialActivity.this, "你拒绝该应用设置悬浮窗", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        //前往应用详情中心
        findViewById(R.id.btn_goto_permission).setOnClickListener(v -> {
            PermissionHelper.getInstance().gotoAppDetail();
        });
    }
}
