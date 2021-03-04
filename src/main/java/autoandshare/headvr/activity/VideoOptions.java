package autoandshare.headvr.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.videolan.libvlc.MediaPlayer;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.VideoProperties;
import autoandshare.headvr.lib.VideoType;

public class VideoOptions extends AppCompatActivity {

    private static final String TAG = "VideoOptions";
    public static String propertyKey;
    public static MediaPlayer.TrackDescription[] audioTracks;
    public static MediaPlayer.TrackDescription[] subtitleTracks;

    private void addRadioButton(RadioGroup radioGroup, String text, boolean checked, Runnable work) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(text);
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                work.run();
            }
        });
        radioGroup.addView(radioButton);
        radioButton.setChecked(checked); // this should be after "addView"
    }

    private void initTrackRadioGroup(RadioGroup group, int pref,
                                     MediaPlayer.TrackDescription[] tracks,
                                     VideoActivity.Consumer<Integer> putVal) {
        if (tracks == null || tracks.length == 0) {
            return;
        }

        addRadioButton(group, "Auto", pref == VideoProperties.TrackAuto,
                () -> putVal.accept(VideoProperties.TrackAuto));
        for (MediaPlayer.TrackDescription t : tracks) {
            addRadioButton(group, t.name, pref == t.id,
                    () -> putVal.accept(t.id));
        }
    }

    private <T> void initEnumRadioGroup(RadioGroup radioGroup, T[] Values,
                                    T selection, VideoActivity.Consumer<T> putVal) {
        for (T val : Values) {
            addRadioButton(radioGroup, val.toString(), val.equals(selection),
                    () -> putVal.accept(val) );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_options);
        if (propertyKey == null) {
            return;
        }
        VideoType videoType = VideoProperties.getVideoType(propertyKey);
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type),
                VideoType.Type.values(),
                videoType.type,
                (x) -> { videoType.type = x; VideoProperties.setVideoType(propertyKey, videoType); }
        );
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type_layout),
                VideoType.Layout.values(),
                videoType.layout,
                (x) -> { videoType.layout = x; VideoProperties.setVideoType(propertyKey, videoType); }
        );
        initEnumRadioGroup(findViewById(R.id.radio_group_video_type_aspect),
                VideoType.Aspect.values(),
                videoType.aspect,
                (x) -> { videoType.aspect = x; VideoProperties.setVideoType(propertyKey, videoType); }
        );
        initTrackRadioGroup(findViewById(R.id.radio_group_audio_tracks),
                VideoProperties.getVideoAudio(propertyKey),
                audioTracks,
                (x) -> VideoProperties.setVideoAudio(propertyKey, x)
        );
        initTrackRadioGroup(findViewById(R.id.radio_group_subtitle_tracks),
                VideoProperties.getVideoSubtitle(propertyKey),
                subtitleTracks,
                (x) -> VideoProperties.setVideoSubtitle(propertyKey, x)
        );
    }

    public void closeButtonPressed(View v) {
        finish();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "video type: " + VideoProperties.getVideoType(propertyKey));
        Log.d(TAG, "audio track: " + VideoProperties.getVideoAudio(propertyKey));
        Log.d(TAG, "subtitle track: " + VideoProperties.getVideoSubtitle(propertyKey));
        super.onPause();
    }
}