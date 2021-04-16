package autoandshare.headvr.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.app.ActivityCompat;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.net.URLDecoder;
import java.util.List;

public class VlcHelper {
    private static final String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    // Internet and Storage Permissions
    public static void verifyPermissions(Activity activity) {
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

    public static class VlcSelection {
        public String listUrl;
        public List<MediaWrapper> list;
        public int position;
        public MediaWrapper mw;
        public Context context;

        public VlcSelection(String listUrl, List<MediaWrapper> list, int position,
                            MediaWrapper mw, Context context) {
            this.listUrl = listUrl;
            this.list = list;
            this.position = position;
            this.mw = mw;
            this.context = context;
        }
    }

    public static VlcSelection Selection;

    public static void openMedia(Context context, Intent intent) {
        Uri uri = intent.getData();
        openUri(context, uri);
    }
    public static void openUri(Context context, Uri uri) {
        if (uri.toString().contains("%2F")) {
            try {
                uri = Uri.parse(URLDecoder.decode(uri.toString(), "UTF-8"));
            } catch (Exception ex) {
            }
        }
        MediaWrapper mw = MLServiceLocator.getAbstractMediaWrapper(uri);
        openMedia(context, mw);
    }

    public static void openMedia(Context context, MediaWrapper mw) {
        Selection = new VlcSelection(null, null, -1, mw, context);
        startActivity(context, mw.getUri());
    }

    public static void openList(String mrl, Context context, List<MediaWrapper> list, int position) {
        if (list.size() == 0) {
            return;
        }
        Selection = new VlcSelection(mrl, list, position, null, context);
        startActivity(context, list.get(0).getUri());
    }

    private static void startActivity(Context context, Uri uri) {
        Intent i = new Intent(context, VideoActivity.class);
        i.setData(uri);
        context.startActivity(i);
    }
}
