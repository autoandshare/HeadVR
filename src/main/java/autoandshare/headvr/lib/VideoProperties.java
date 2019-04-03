package autoandshare.headvr.lib;

import android.content.SharedPreferences;

import com.tencent.mmkv.MMKV;

public class VideoProperties {
    private MMKV prefPostion;
    private SharedPreferences.Editor editorPosition;
    private MMKV prefForce2D;
    private SharedPreferences.Editor editorForce2D;

    public VideoProperties() {

        prefPostion = MMKV.mmkvWithID("VideoProperties-Position");
        editorPosition = prefPostion.edit();
        prefForce2D = MMKV.mmkvWithID("VideoProperties-Force2D");
        editorForce2D = prefForce2D.edit();
    }

    public float getPosition(String key) {
        float position = prefPostion.getFloat(key, 0);
        if ((position < 0) || (position > 1)) {
            position = 0;
        }
        return position;
    }

    public void setPosition(String key, float position) {
        if (position == 0) {
            editorPosition.remove(key);
        } else {
            editorPosition.putFloat(key, position);
        }
        editorPosition.apply();
    }

    public boolean getForce2D(String key) {
        return prefForce2D.getBoolean(key, false);
    }

    public void setForce2D(String key, boolean force2D) {
        if (force2D) {
            editorForce2D.putBoolean(key, true);
        } else {
            editorForce2D.remove(key);
        }
        editorForce2D.apply();
    }
}
