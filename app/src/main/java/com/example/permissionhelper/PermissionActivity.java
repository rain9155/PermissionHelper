package com.example.permissionhelper;

import android.Manifest;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.permission.PermissionHelper;
import com.example.permission.bean.Permission;
import com.example.permission.callback.IPermissionCallback;

public class PermissionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        findViewById(R.id.btn_check_permission).setOnClickListener(v -> {
                if(PermissionHelper.getInstance().checkPermission(Manifest.permission.CALL_PHONE)){
                    Toast.makeText(PermissionActivity.this, "你已经同意了该权限", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(PermissionActivity.this, "你还没有同意该权限", Toast.LENGTH_SHORT).show();
                }
        });
        findViewById(R.id.btn_call_phone).setOnClickListener(v -> {
            PermissionHelper.getInstance().with(this).requestPermission(
                    Manifest.permission.CALL_PHONE,
                    new IPermissionCallback() {
                        @Override
                        public void onAccepted(Permission permission) {
                            Toast.makeText(PermissionActivity.this, "你同意了" + permission.name + "权限", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDenied(Permission permission) {
                            Toast.makeText(PermissionActivity.this, "你拒绝了" + permission.name + "权限，打电话要同意该权限才能使用哦·", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDeniedAndReject(Permission permission) {
                            new AlertDialog.Builder(PermissionActivity.this)
                                    .setTitle("提示")
                                    .setMessage("是否前往权限管理中心同意该权限")
                                    .setPositiveButton("确认", (dialog1, which) -> {
                                        PermissionHelper.getInstance().gotoPermissionDetail();
                                    })
                                    .setNegativeButton("取消", (dialog12, which) -> {
                                        dialog12.dismiss();
                                    })
                                    .create().show();
                        }
                    }
            );
        });
    }
}
