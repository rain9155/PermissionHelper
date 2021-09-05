package com.example.permissionhelper;

import android.Manifest;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import com.example.permission.IRejectedCallback;
import com.example.permission.IRejectedForeverCallback;
import com.example.permission.PermissionHelper;
import com.example.permissionhelper.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private final List<String> mPermissions = Arrays.asList(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.REQUEST_INSTALL_PACKAGES,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.PACKAGE_USAGE_STATS,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: orientation = " + this.getRequestedOrientation());

        ActivityMainBinding binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        binding.btnRequestPermission.setOnClickListener(v -> {
            PermissionHelper.with(MainActivity.this)
                .permissions(mPermissions)
                .request(MainActivity.this::showRequestPermissionsResult);
        });

        binding.btnRequestPermissionExplain.setOnClickListener(v -> {
            PermissionHelper.with(this)
                .permissions(mPermissions)
                .explainBeforeRequest((process, requestPermissions) -> {
                    showAlert(
                        "以下申请的权限是运行时必须的，是否继续申请",
                        getPermissionsLabel(requestPermissions),
                        "否",
                        (DialogInterface.OnClickListener) (dialog, which) -> {
                            process.requestTermination();
                        },
                        "是",
                        (dialog, which) -> {
                            process.requestContinue();
                        }
                    );
                }).explainAfterRejected((IRejectedCallback) (process, rejectedPermissions) -> {
                    showAlert(
                        "以下点击拒绝的权限是运行时必须的，请申请后再进行后续操作",
                        getPermissionsLabel(rejectedPermissions),
                        "不申请",
                        (DialogInterface.OnClickListener) (dialog, which) -> {
                            process.requestTermination();
                        },
                        "申请",
                        (dialog, which) -> {
                            process.requestAgain(rejectedPermissions);
                        }
                    );
                }).explainAfterRejectedForever((IRejectedForeverCallback) (process, rejectedForeverPermissions) -> {
                    showAlert(
                        "以下点击永久拒绝的权限是运行时必须的，请前往权限中心同意",
                        getPermissionsLabel(rejectedForeverPermissions),
                        "不前往",
                        (DialogInterface.OnClickListener) (dialog, which) -> {
                            process.requestTermination();
                        },
                        "前往",
                        (dialog, which) -> {
                            process.gotoSettings();
                        }
                    );
                }).request(this::showRequestPermissionsResult);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode = " + requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode = " + requestCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void showAlertWithNotNegative(String title, List<CharSequence> items, String positive, DialogInterface.OnClickListener onPositiveClick){
        showAlert(title, items, "", null, positive, onPositiveClick);
    }

    private void showAlert(String title, List<CharSequence> items, String negative, DialogInterface.OnClickListener onNegativeClick, String positive, DialogInterface.OnClickListener onPositiveClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(title)
                .setItems(items.toArray(new CharSequence[]{}), null);
        if(!TextUtils.isEmpty(negative)){
            builder.setNegativeButton(negative, (dialog, which) -> {
                if (onNegativeClick != null) {
                    onNegativeClick.onClick(dialog, which);
                }
                dialog.dismiss();
            });
        }
        if(!TextUtils.isEmpty(positive)){
            builder.setPositiveButton(positive, (dialog, which) -> {
                if (onPositiveClick != null) {
                    onPositiveClick.onClick(dialog, which);
                }
                dialog.dismiss();
            });
        }
        builder.create().show();
    }

    private List<CharSequence> getPermissionsLabel(List<String> permissions) {
        List<CharSequence> labels = new ArrayList<>(permissions.size());
        for (String permission : permissions) {
            try {
                PermissionInfo permissionInfo = this.getPackageManager().getPermissionInfo(permission, PackageManager.GET_META_DATA);
                labels.add(permissionInfo.loadLabel(this.getPackageManager()));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                labels.add("未知权限");
            }
        }
        return labels;
    }

    private void showRequestPermissionsResult(boolean isAllGrant, List<String> grantedPermissions, List<String> rejectedPermissions) {
        Log.d(TAG, "onPermissionResult: isAllGrant = " + isAllGrant + ", grantedPermissions = " + grantedPermissions + ", rejectedPermissions" + rejectedPermissions);
        if (isAllGrant) {
            showAlertWithNotNegative(
                "你同意了所有权限",
                    Collections.emptyList(),
                "确定",
                null
            );
        } else {
            showAlertWithNotNegative(
                "权限请求结果，你拒绝了以下权限",
                getPermissionsLabel(rejectedPermissions),
                "确定",
                null
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.item_start_permission_activity){
            startActivity(new Intent(this, PermissionGroupActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
