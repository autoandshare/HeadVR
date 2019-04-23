package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;

import com.tencent.mmkv.MMKV;

import org.videolan.libvlc.Media;
import org.videolan.medialibrary.media.MediaWrapper;

import java.util.ArrayList;
import java.util.List;


import autoandshare.headvr.activity.VlcHelper;

public class PlayList {
    public interface ListSource {
        public List<MediaWrapper> loadList();

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

    private static void updatePlayList(PlayList playList, List<MediaWrapper> list, int currentPos, String listPositionKey) {
        playList.list = list;
        playList.currentPos = currentPos;
        playList.listPositionKey = listPositionKey;
        playList.loadDone();
        playList.isReady = true;
    }

    public static PlayList getPlayList(Uri uri, Activity activity) {
        PlayList playList = new PlayList();

        ListSource source = getSource(uri, activity);

        if (source == null) {
            List<MediaWrapper> list = new ArrayList<>();
            list.add(new MediaWrapper(uri));
            updatePlayList(playList, list, 0, uri.toString());
        } else {
            // use thread to work around NetworkOnMainThreadException
            new Thread(() -> {
                updatePlayList(playList, source.loadList(), source.getPosition(), source.getPositionKey());
            }).start();
        }
        return playList;
    }

    public static boolean isListFile(String fileName) {
        return fileName.endsWith(".list");
    }

    private List<MediaWrapper> list;
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

    public MediaWrapper next(int offset) {
        if ((!isReady) || (list == null)) {
            return null;
        }
        currentPos = currentPos + offset;

        currentPos = ((currentPos % list.size()) + list.size()) % list.size();

        listPosition.putInt(listPositionKey, currentPos);

        return list.get(currentPos);

    }

    public String currentIndex() {
        if ((!isReady) || (list == null)) {
            return "";
        }
        return "" + (currentPos + 1) + "/" + list.size() + " ";
    }
}
