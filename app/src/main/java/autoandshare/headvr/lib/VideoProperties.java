package autoandshare.headvr.lib;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;

public class VideoProperties {
    private SharedPreferences prefPostion;
    private SharedPreferences.Editor editorPosition;
    private SharedPreferences prefForce2D;
    private SharedPreferences.Editor editorForce2D;

    public VideoProperties(Activity activity) {
        prefPostion = activity.getSharedPreferences("VideoProperties", 0);
        editorPosition = prefPostion.edit();
        prefForce2D = activity.getSharedPreferences("VideoProperties-Force2D", 0);
        editorForce2D = prefForce2D.edit();
    }

    public int getPosition(Uri uri) {
        return prefPostion.getInt(uri.toString(), 0);
    }

    public void setPosition(Uri uri, int positon) {
        editorPosition.putInt(uri.toString(), positon);
        editorPosition.apply();
    }

    public boolean getForce2D(Uri uri) {
        return prefForce2D.getBoolean(uri.toString(), false);
    }
    public void setForce2D(Uri uri, boolean force2D) {
        if (force2D) {
            editorForce2D.putBoolean(uri.toString(), true);
        }
        else {
            editorForce2D.remove(uri.toString());
        }
        editorForce2D.apply();
    }
}
