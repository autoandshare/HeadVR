package autoandshare.headvr.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import autoandshare.headvr.R;
import autoandshare.headvr.lib.Setting;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    Setting setting;

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSeekBars();
    }

    void initSeekBars(int id, Setting.id propertyName, int textId) {
        SeekBar seekBar = findViewById(id);
        TextView textView = findViewById(textId);

        seekBar.setMax(setting.getMax(propertyName) - setting.getMin(propertyName));

        seekBar.setProgress(setting.get(propertyName) - setting.getMin(propertyName));
        textView.setText(String.format("%s (%d)", setting.getDescription(propertyName), setting.get(propertyName)));


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = progress + setting.getMin(propertyName);
                setting.set(propertyName, newValue);
                textView.setText(String.format("%s (%d)", setting.getDescription(propertyName), newValue));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private String readAsset(String name) {
        StringBuilder buf = new StringBuilder();

        try (InputStream stream = getAssets().open(name)) {
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                buf.append(line).append("\n");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setting = new Setting(this);

        setContentView(R.layout.setting_ui);

        Button button = findViewById(R.id.buttonReset);
        button.setOnClickListener(this);

        initSeekBars();

        WebView help = findViewById(R.id.help);
        help.loadData(readAsset("help.html"), "text/html", "utf-8");
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
