package autoandshare.headvr.lib;

import android.content.SharedPreferences;

import com.tencent.mmkv.MMKV;

import java.util.Set;

public class VideoProperties {
    private static MMKV prefPostion = MMKV.mmkvWithID("VideoProperties-Position");
    private static MMKV prefForce2D = MMKV.mmkvWithID("VideoProperties-Force2D");
    private static MMKV prefVideoType = MMKV.mmkvWithID("VideoProperties-VideoType");
    private static MMKV prefVideoTypeLayout = MMKV.mmkvWithID("VideoProperties-VideoTypeLayout");
    private static MMKV prefVideoTypeAspect = MMKV.mmkvWithID("VideoProperties-VideoTypeAspect");
    private static MMKV prefAudio = MMKV.mmkvWithID("VideoProperties-Audio");
    private static MMKV prefSubtitle = MMKV.mmkvWithID("VideoProperties-Subtitle");
    private static MMKV prefEyeDistance = MMKV.mmkvWithID("VideoProperties-EyeDistance");

    public static float getVideoEyeDistanceFloat(String key) {
        return Setting.Instance.getFloatValue(Setting.id.EyeDistance,
                getVideoEyeDistance(key));
    }
    public static int getVideoEyeDistance(String key) {
        return prefEyeDistance.getInt(key, Setting.Instance.get(Setting.id.EyeDistance));
    }
    public static void setVideoEyeDistance(String key, int val) {
        prefEyeDistance.putInt(key, val);
    }

    public static int TrackAuto = -100;

    public static int getVideoAudio(String key) {
        return prefAudio.getInt(key, TrackAuto);
    }

    public static void setVideoAudio(String key, int val) {
        prefAudio.putInt(key, val);
    }

    public static int getVideoSubtitle(String key) {
        return prefSubtitle.getInt(key, TrackAuto);
    }

    public static void setVideoSubtitle(String key, int val) {
        prefSubtitle.putInt(key, val);
    }

    public static VideoType getVideoType(String key) {
        VideoType videoType = new VideoType();
        videoType.type = VideoType.Type.valueOf(prefVideoType.getString(key, "Auto"));
        videoType.layout = VideoType.Layout.valueOf(prefVideoTypeLayout.getString(key, "Auto"));
        videoType.aspect = VideoType.Aspect.valueOf(prefVideoTypeAspect.getString(key, "Auto"));
        return videoType;
    }

    public static void setVideoType(String key, VideoType videoType) {
        prefVideoType.putString(key, videoType.type.name());
        prefVideoTypeLayout.putString(key, videoType.layout.name());
        prefVideoTypeAspect.putString(key, videoType.aspect.name());
    }

    public static float getPosition(String key) {
        float position = prefPostion.getFloat(key, 0);
        if ((position < 0) || (position > 1)) {
            position = 0;
        }
        return position;
    }

    public static void setPosition(String key, float position) {
        if (position == 0) {
            prefPostion.remove(key);
        } else {
            prefPostion.putFloat(key, position);
        }
    }

    public static boolean getForce2D(String key) {
        return prefForce2D.getBoolean(key, false);
    }

    public static void setForce2D(String key, boolean force2D) {
        if (force2D) {
            prefForce2D.putBoolean(key, true);
        } else {
            prefForce2D.remove(key);
        }
    }
}
