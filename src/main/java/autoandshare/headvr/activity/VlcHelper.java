package autoandshare.headvr.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.videolan.libvlc.LibVLC;
import org.videolan.medialibrary.media.MediaWrapper;

import java.util.List;

public class VlcHelper {
    public static class VlcSelection {
        public String listUrl;
        public List<MediaWrapper> list;
        public int position;
        public MediaWrapper mw;

        public VlcSelection(String listUrl, List<MediaWrapper> list, int position, MediaWrapper mw) {
            this.listUrl = listUrl;
            this.list = list;
            this.position = position;
            this.mw = mw;
        }
    }

    public static LibVLC Instance;
    public static VlcSelection Selection;

    public static void openMedia(Context context, MediaWrapper mw, LibVLC vlc) {
        Instance = vlc;
        Selection = new VlcSelection(null, null, -1, mw);
        startActivity(context, mw.getUri());
    }

    public static void openList(String mrl, Context context, List<MediaWrapper> list, int position, LibVLC vlc) {
        Instance = vlc;
        Selection = new VlcSelection(mrl, list, position, null);
        startActivity(context, list.get(0).getUri());
    }

    private static void startActivity(Context context, Uri uri) {
        Intent i = new Intent(context, VideoActivity.class);
        i.setData(uri);
        context.startActivity(i);
    }
}
