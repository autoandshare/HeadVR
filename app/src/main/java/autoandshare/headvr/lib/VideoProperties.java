package autoandshare.headvr.lib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;

public class VideoProperties {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public VideoProperties(Activity activity) {
        pref = activity.getSharedPreferences("VideoProperties", 0);
        editor = pref.edit();
    }

    public int getPosition(Uri uri) {
        return pref.getInt(uri.toString(), 0);
    }

    public void setPosition(Uri uri, int positon) {
        editor.putInt(uri.toString(), positon);
        editor.apply();
    }
}
