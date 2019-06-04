package com.example.permissionhelper;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.permission.PermissionHelper;
import com.example.permission.bean.Permission;
import com.example.permission.callback.IPermissionCallback;
import com.example.permission.callback.IPermissionsCallback;

import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSION_PHONE = 0x000;
    private final int REQUEST_CODE_PERMISSIONS = 0x001;

    private String [] mPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        //单个权限
        findViewById(R.id.btn_check_permission).setOnClickListener(v -> {
                if(PermissionHelper.getInstance().with(this).checkPermission(Manifest.permission.CALL_PHONE)){
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
                            makePhone();
                        }

                        @Override
                        public void onDenied(Permission permission) {
                            Toast.makeText(PermissionActivity.this, "你拒绝了" + permission.name + "权限，打电话要同意该权限才能使用哦·", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDeniedAndReject(Permission permission) {
                            new AlertDialog.Builder(PermissionActivity.this)
                                    .setTitle("提示")
                                    .setMessage("是否前往权限管理中心同意该权限?")
                                    .setPositiveButton("确认", (dialog1, which) -> {
                                        PermissionHelper.getInstance().with(PermissionActivity.this).gotoPermissionDetail(REQUEST_CODE_PERMISSION_PHONE);
                                    })
                                    .setNegativeButton("取消", (dialog12, which) -> {
                                        dialog12.dismiss();
                                    })
                                    .create().show();
                        }
                    }
            );
        });

        //多个权限
        mPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.CAMERA};
        findViewById(R.id.btn_check_permissions).setOnClickListener(v -> {
            String[] rejectedPermissions = PermissionHelper.getInstance().with(this).checkPermissions(mPermissions);
            if(rejectedPermissions.length == 0){
                Toast.makeText(PermissionActivity.this, "所有权限已经同意", Toast.LENGTH_SHORT).show();
            }else {
                StringBuffer buffer = new StringBuffer();
                for(String permission : rejectedPermissions){
                    buffer.append(permission).append(",");
                }
                buffer.deleteCharAt(buffer.length() - 1);
                Toast.makeText(PermissionActivity.this, buffer.toString() + "权限还没有被同意", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_get_permissions).setOnClickListener(v -> {
            PermissionHelper.getInstance().with(this).requestPermissions(
                    mPermissions,
                    new IPermissionsCallback() {
                        @Override
                        public void onAccepted(List<Permission> permissions) {
                            StringBuffer buffer = new StringBuffer();
                            for(Permission permission : permissions){
                                buffer.append(permission.name).append(",");
                            }
                            buffer.deleteCharAt(buffer.length() - 1);
                            Toast.makeText(PermissionActivity.this, "你同意了" + buffer.toString() + "权限", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDenied(List<Permission> permissions) {
                            StringBuffer buffer = new StringBuffer();
                            for(Permission permission : permissions){
                                buffer.append(permission.name).append(",");
                            }
                            buffer.deleteCharAt(buffer.length() - 1);
                            new AlertDialog.Builder(PermissionActivity.this)
                                    .setTitle("提示")
                                    .setMessage("是否前往权限管理中心同意" + buffer.toString() + "权限? 否则相应功能无法使用")
                                    .setPositiveButton("确认", (dialog1, which) -> {
                                        PermissionHelper.getInstance().gotoPermissionDetail(REQUEST_CODE_PERMISSIONS);
                                    })
                                    .setNegativeButton("取消", (dialog12, which) -> {
                                        dialog12.dismiss();
                                        finish();
                                    })
                                    .create().show();
                        }

                        //这里可以根据业务逻辑选择重写onDeniedAndReject（）
                    }
            );
        });

        //前往权限管理中心
        findViewById(R.id.btn_goto_permission).setOnClickListener(v -> {
            PermissionHelper.getInstance().with(this).gotoPermissionDetail();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //这里根据requestCode再检查一遍权限，如果用户在权限管理中心同意该权限，就可以继续做想做的事
        if(requestCode == REQUEST_CODE_PERMISSION_PHONE){
            if(PermissionHelper.getInstance().with(this).checkPermission(Manifest.permission.CALL_PHONE)){
                makePhone();
            }
        }
        //这里再次检查多个权限的同意情况，在这里根据同意的权限做相应的逻辑
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            String[] rejectedPermissions = PermissionHelper.getInstance().with(this).checkPermissions(mPermissions);
            if(rejectedPermissions.length == 0){
                Toast.makeText(PermissionActivity.this, "所有权限已经同意", Toast.LENGTH_SHORT).show();
            }else {
                StringBuffer buffer = new StringBuffer();
                for(String permission : rejectedPermissions){
                    buffer.append(permission).append(",");
                }
                buffer.deleteCharAt(buffer.length() - 1);
                Toast.makeText(PermissionActivity.this, buffer.toString() + "权限还没有被同意, 无法使用应用！", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void makePhone() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:10086"));
        startActivity(new Intent(intent));
    }
}
