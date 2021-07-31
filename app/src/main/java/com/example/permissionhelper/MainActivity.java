package com.example.permissionhelper;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_permission).setOnClickListener(v -> startActivity(new Intent(this, PermissionActivity.class)));
        findViewById(R.id.btn_special_permission).setOnClickListener(v -> startActivity(new Intent(this, SpecialActivity.class)));
    }
}
