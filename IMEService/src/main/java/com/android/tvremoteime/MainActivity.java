package com.android.tvremoteime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.tvremoteime.server.RemoteServer;
import com.android.tvremoteime.adb.AdbHelper;
import com.android.tvremoteime.mouse.MouseAccessibilityService;
import com.zxt.dlna.dmr.ZxtMediaRenderer;

public class MainActivity extends Activity implements View.OnClickListener {

    private ImageView qrCodeImage;
    private TextView addressView;
    private EditText dlnaNameText;
    private TextView accessibilityStatusView;
    private Button accessibilityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrCodeImage = this.findViewById(R.id.ivQRCode);
        addressView = this.findViewById(R.id.tvAddress);
        dlnaNameText = this.findViewById(R.id.etDLNAName);
        accessibilityStatusView = this.findViewById(R.id.tvAccessibilityStatus);
        accessibilityButton = this.findViewById(R.id.btnAccessibility);

        this.setTitle(this.getResources().getString( R.string.app_name) + "  V" + AppPackagesHelper.getCurrentPackageVersion(this));
        dlnaNameText.setText(DLNAUtils.getDLNANameSuffix(this.getApplicationContext()));

        // 设置按钮点击监听器
        findViewById(R.id.btnUseIME).setOnClickListener(this);
        findViewById(R.id.btnSetIME).setOnClickListener(this);
        findViewById(R.id.btnStartService).setOnClickListener(this);
        findViewById(R.id.btnSetDLNA).setOnClickListener(this);
        if (accessibilityButton != null) {
            accessibilityButton.setOnClickListener(this);
        }

        refreshQRCode();
        updateAccessibilityStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessibilityStatus();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnUseIME) {
            openInputMethodSettings();
            if(Environment.isEnableIME(this)){
                Environment.toast(getApplicationContext(), "太棒了，您已经激活启用了" + getString(R.string.keyboard_name) +"输入法！");
            }
        } else if (id == R.id.btnSetIME) {
            if(!Environment.isEnableIME(this)) {
                Environment.toast(getApplicationContext(), "抱歉，请您先激活启用" + getString(R.string.keyboard_name) +"输入法！");
                openInputMethodSettings();
                if(!Environment.isEnableIME(this)) return;
            }
            try {
                ((InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
            }catch (Exception ignored) {
                Environment.toast(getApplicationContext(), "抱歉，无法设置为系统默认输入法，请手动启动服务！");
            }
            if(Environment.isDefaultIME(this)){
                Environment.toast(getApplicationContext(), "太棒了，" + getString(R.string.keyboard_name) +"已是系统默认输入法！");
            }
        } else if (id == R.id.btnStartService) {
            // 使用显式Intent启动服务 (Android 5.0+要求)
            startService(new Intent(this, IMEService.class));
            if(!Environment.isDefaultIME(this)) {
                if (AdbHelper.getInstance() == null) AdbHelper.createInstance();
            }
            Environment.toast(getApplicationContext(), "服务已手动启动，稍后可尝试访问控制端页面");
        } else if (id == R.id.btnSetDLNA) {
            DLNAUtils.setDLNANameSuffix(this.getApplicationContext(), dlnaNameText.getText().toString());
        } else if (id == R.id.btnAccessibility) {
            openAccessibilitySettings();
        }
        refreshQRCode();
        updateAccessibilityStatus();
    }

    private void openInputMethodSettings(){
        try {
            this.startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
        }catch (Exception ignored){
            Environment.toast(getApplicationContext(), "抱歉，无法激活启用输入法，请手动启动服务！");
        }
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Environment.toast(getApplicationContext(), "请在列表中找到\"" + getString(R.string.app_name) + "\"并启用");
        } catch (Exception e) {
            Environment.toast(getApplicationContext(), "无法打开辅助功能设置，请手动前往：设置 → 辅助功能");
        }
    }

    private void updateAccessibilityStatus() {
        boolean isEnabled = isAccessibilityServiceEnabled();
        if (accessibilityStatusView != null) {
            if (isEnabled) {
                accessibilityStatusView.setText("已启用");
                accessibilityStatusView.setTextColor(0xFF4CAF50); // 绿色
            } else {
                accessibilityStatusView.setText("未启用");
                accessibilityStatusView.setTextColor(0xFFFF5722); // 橙色
            }
        }
        if (accessibilityButton != null) {
            accessibilityButton.setText(isEnabled ? "已启用" : "去设置");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        // 首先检查服务实例是否存在
        if (MouseAccessibilityService.isServiceEnabled()) {
            return true;
        }
        // 然后检查系统设置
        String serviceName = getPackageName() + "/" + MouseAccessibilityService.class.getCanonicalName();
        try {
            String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (!TextUtils.isEmpty(enabledServices)) {
                return enabledServices.contains(serviceName);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }

    private void refreshQRCode(){
        String address = RemoteServer.getServerAddress(this);
        addressView.setText(address);
        qrCodeImage.setImageBitmap(QRCodeGen.generateBitmap(address, 150, 150));
    }




}
