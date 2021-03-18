package autoandshare.headvr.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
        initLangKeywords(view);
    }

    private void initLangKeywords(View view) {
        initLangKeyword(view, R.id.audioLangKeywordSwitch, R.id.audioLangKeywords,
                Setting.id.AudioLanguageKeywords);
        initLangKeyword(view, R.id.subtitleLangKeywordSwitch, R.id.subtitleLangKeywords,
                Setting.id.SubtitleLanguageKeywords);
    }

    private void initLangKeyword(View view, int switchId, int editId, Setting.id settingId) {
        Switch sw = view.findViewById(switchId);
        EditText edit = view.findViewById(editId);

        boolean enabled = setting.getBoolean(settingId);
        sw.setChecked(enabled);
        edit.setEnabled(enabled);
        edit.setText(setting.getString(settingId));

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setting.putBoolean(settingId, isChecked);
                edit.setEnabled(isChecked);
            }
        });
        edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setting.putString(settingId, s.toString());
            }
        });

    }

    private void initSwitchs(View view) {
        initSwitch(view, R.id.distortionCorrectionSwitch, Setting.id.EnableDistortionCorrection);
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
        initSeekBar(view, R.id.seekBarBrightness, Setting.id.Brightness, R.id.textViewBrightness);
        initSeekBar(view, R.id.seekBarEyeDistance, Setting.id.EyeDistance, R.id.textViewEyeDistance);
        initSeekBar(view, R.id.seekBarVerticalDistance, Setting.id.VerticalDistance, R.id.textViewVerticalDistance);
        initSeekBar(view, R.id.seekBarVideoSize, Setting.id.VideoSize, R.id.textViewVideoSize);
        initSeekBar(view, R.id.seekBarSensitivity, Setting.id.MotionSensitivity, R.id.textViewSensitivity);
    }

    private void initSeekBar(View view, int id, Setting.id propertyName, int textId) {
        initSeekBar(view, id, propertyName, textId,
                Setting.Instance.get(propertyName),
                (x) -> Setting.Instance.set(propertyName, x),
                propertyName != Setting.id.EyeDistance ? "%s (%d)" : "%s (default) (%d)");
    }

    public static void initSeekBar(View view, int id, Setting.id propertyName, int textId,
                                   int currentVal,
                                   VideoActivity.Consumer<Integer> setValue,
                                   String formatString) {
        SeekBar seekBar = view.findViewById(id);
        TextView textView = view.findViewById(textId);

        seekBar.setMax(Setting.Instance.getMax(propertyName) - Setting.Instance.getMin(propertyName));

        seekBar.setProgress(currentVal - Setting.Instance.getMin(propertyName));
        textView.setText(String.format(formatString, Setting.Instance.getDescription(propertyName), currentVal));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newValue = progress + Setting.Instance.getMin(propertyName);
                setValue.accept(newValue);
                textView.setText(String.format(formatString, Setting.Instance.getDescription(propertyName), newValue));
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
