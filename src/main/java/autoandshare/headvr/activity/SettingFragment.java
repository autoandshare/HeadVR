package autoandshare.headvr.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.Setting;

public class SettingFragment extends Fragment implements View.OnClickListener {

    Setting setting;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.setting_ui, null);
        return root;
    }

    public void onActivityCreated(Bundle savedInstanceState) {

        // you can add listener of elements here
        super.onActivityCreated(savedInstanceState);

        setting = new Setting(getActivity());

        View view = getView();

        Button button = view.findViewById(R.id.buttonReset);
        button.setOnClickListener(this);

        initUI();
    }

    private void initUI() {
        View view = getView();
        initSeekBars(view);
        initSwitchs(view);
    }

    private void initSwitchs(View view) {
        initSwitch(view, R.id.distortionCorrectionSwitch, Setting.id.DisableDistortionCorrection);
        initSwitch(view, R.id.headControlSwitch, Setting.id.HeadControl);
    }

    private void initSwitch(View view, int id, Setting.id settingId) {
        Switch sw = view.findViewById(id);
        sw.setChecked(setting.getBoolean(settingId));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setting.putBoolean(settingId, isChecked);
            }
        });
    }

    private void initSeekBars(View view) {
        initSeekBars(view, R.id.seekBarBrightness, Setting.id.Brightness, R.id.textViewBrightness);
        initSeekBars(view, R.id.seekBarEyeDistance, Setting.id.EyeDistance, R.id.textViewEyeDistance);
        initSeekBars(view, R.id.seekBarEyeDistance3D, Setting.id.EyeDistance3D, R.id.textViewEyeDistance3D);
        initSeekBars(view, R.id.seekBarVerticalDistance, Setting.id.VerticalDistance, R.id.textViewVerticalDistance);
        initSeekBars(view, R.id.seekBarVideoSize, Setting.id.VideoSize, R.id.textViewVideoSize);
        initSeekBars(view, R.id.seekBarSensitivity, Setting.id.MotionSensitivity, R.id.textViewSensitivity);
    }

    void initSeekBars(View view, int id, Setting.id propertyName, int textId) {
        SeekBar seekBar = view.findViewById(id);
        TextView textView = view.findViewById(textId);

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


    @Override
    public void onClick(View view) {
        // reset values
        setting.clear();
        initUI();
    }
}
