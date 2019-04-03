package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;

import com.tencent.mmkv.MMKV;

import java.util.List;


import autoandshare.headvr.activity.VlcHelper;

public class PlayList {
    public interface ListSource {
        public List<Uri> loadList();

        public int getPosition();

        public String getPositionKey();
    }

    private static ListSource getSource(Uri uri, Activity activity) {
        ListSource source = null;
        if (VlcHelper.Selection != null) {
            source = new VlcMediaList(VlcHelper.Selection);
            VlcHelper.Selection = null;
        } else if (isListFile(uri.getPath())) {
            source = new URLFileList(uri, activity);
        }
        return source;
    }

    public static PlayList getPlayList(Uri uri, Activity activity) {
        ListSource source = getSource(uri, activity);

        if (source == null) {
            return null;
        }

        PlayList playList = new PlayList();

        // use thread to work around NetworkOnMainThreadException
        new Thread(() -> {
            playList.list = source.loadList();
            playList.currentPos = source.getPosition();
            playList.listPositionKey = source.getPositionKey();
            playList.loadDone();
            playList.isReady = true;
        }).start();

        return playList;
    }

    public static boolean isListFile(String fileName) {
        return fileName.endsWith(".list");
    }

    private List<Uri> list;
    private String listPositionKey;
    private int currentPos = -1;

    private MMKV listPosition;
    private boolean isReady = false;

    private void loadDone() {
        if (list == null) {
            return;
        }
        if (list.size() == 0) {
            list = null;
            return;
        }
        listPosition = MMKV.mmkvWithID("List-Position");
        if (currentPos < 0) {
            currentPos = listPosition.getInt(listPositionKey, 0);
        }
    }

    public boolean isReady() {
        return isReady;
    }

    public Uri current() {
        if ((!isReady) || (list == null)) {
            return null;
        }

        currentPos = ((currentPos % list.size()) + list.size()) % list.size();

        listPosition.putInt(listPositionKey, currentPos);

        return list.get(currentPos);
    }

    public Uri next(int offset) {
        if ((!isReady) || (list == null)) {
            return null;
        }
        currentPos = currentPos + offset;
        return current();
    }

    public String currentIndex() {
        if ((!isReady) || (list == null)) {
            return "";
        }
        return "" + (currentPos + 1) + "/" + list.size() + " ";
    }
}
