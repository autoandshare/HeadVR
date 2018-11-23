package autoandshare.headvr.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.Setting;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    Setting setting;

    @Override
    protected void onPause() {
        super.onPause();
        setting.apply();
    }

    private static final String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    // Internet and Storage Permissions
    public static void verifyPermissions(Activity activity, String[] permissions) {
        boolean hasAllPermissions = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                break;
            }
        }

        if (!hasAllPermissions) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    1
            );
        }
    }

    void initSeekBars(int id, Setting.id propertyName, int textId) {
        SeekBar seekBar = findViewById(id);
        TextView textView = findViewById(textId);

        seekBar.setMax(setting.getMax(propertyName) - setting.getMin(propertyName));

        seekBar.setProgress(setting.get(propertyName) - setting.getMin(propertyName));
        textView.setText(String.format("%s (%d)", propertyName, setting.get(propertyName)));


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = progress + setting.getMin(propertyName);
                setting.set(propertyName, newValue);
                textView.setText(String.format("%s (%d)", propertyName, newValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_ui);

        Button button = findViewById(R.id.buttonReset);
        button.setOnClickListener(this);
        setting = new Setting(this);

        initSeekBars();

        verifyPermissions(this, permissions);

    }

    private void initSeekBars() {
        initSeekBars(R.id.seekBarBrightness, Setting.id.Brightness, R.id.textViewBrightness);
        initSeekBars(R.id.seekBarEyeDistance, Setting.id.EyeDistance, R.id.textViewEyeDistance);
        initSeekBars(R.id.seekBarEyeDistance3D, Setting.id.EyeDistance3D, R.id.textViewEyeDistance3D);
        initSeekBars(R.id.seekBarVerticalDistance, Setting.id.VerticalDistance, R.id.textViewVerticalDistance);
        initSeekBars(R.id.seekBarVideoSize, Setting.id.VideoSize, R.id.textViewVideoSize);
        initSeekBars(R.id.seekBarSensitivity, Setting.id.MotionSensitivity, R.id.textViewSensitivity);
    }

    @Override
    public void onClick(View view) {
        // reset values
        setting.clear();
        initSeekBars();
    }
}
