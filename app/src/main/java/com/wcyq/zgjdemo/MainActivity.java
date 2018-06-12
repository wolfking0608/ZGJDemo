package com.wcyq.zgjdemo;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wcyq.zgjdemo.qrcode.decode.CaptureActivity;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_SCAN = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        TextView textView = (TextView) findViewById(R.id.tv);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customScan();
            }
        });
    }

    public void customScan() {
        AndPermission.with(this).permission(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE).callback(new PermissionListener() {
            @Override
            public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                intent.putExtra("portraitOrLandscape", "landscape1");//用来切换横屏的
                intent.putExtra("backText", "返回");
                intent.putExtra("titileText", "扫描信息");
                intent.putExtra("imgText", "相册");
                intent.putExtra("labelText", "请将二维码放入扫描框即可扫描");

                intent.putExtra("headColor", "#FFFFFF");
                intent.putExtra("labelColor", "#FFFFFF");
                intent.putExtra("headSize", 18);
                intent.putExtra("labelSize", 1);


                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }

            @Override
            public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {

                Uri packageURI = Uri.parse("package:" + MainActivity.this.getPackageName());
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(MainActivity.this, "没有权限无法扫描呦", Toast.LENGTH_LONG).show();
            }
        }).start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {

            String result = data.getStringExtra("result");
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        }
    }

}
