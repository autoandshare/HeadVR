package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;

import com.tencent.mmkv.MMKV;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.util.ArrayList;
import java.util.List;


import autoandshare.headvr.activity.VlcHelper;

public class PlayList implements IPlayList {
    private Activity activity;
    private Uri uri;

    private List<MediaWrapper> list;
    private String listPositionKey;
    private int currentPos;
    private MMKV listPosition;

    public PlayList(Uri uri) {
        this.uri = uri;
    }

    @Override
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getIndexString() {
        return list == null ?
                "..." :
                "[" + (currentPos + 1) + "/" + list.size() + "]";
    }

    @Override
    public PlayItem getMediaAtOffset(int offset) {
        if (list == null) {
            initList();
        }
        return new PlayItem(next(offset));
    }

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

    private void updatePlayList(List<MediaWrapper> list, int currentPos, String listPositionKey) {
        this.list = list;
        this.currentPos = currentPos;
        this.listPositionKey = listPositionKey;

        listPosition = MMKV.mmkvWithID("List-Position");
        if (currentPos < 0) {
            this.currentPos = listPosition.getInt(listPositionKey, 0);
        }
    }

    private void initList() {
        ListSource source = getSource(uri, activity);

        if (source == null) {
            List<MediaWrapper> list = new ArrayList<>();
            list.add(MLServiceLocator.getAbstractMediaWrapper(uri));
            updatePlayList(list, 0, uri.toString());
        } else {
            updatePlayList(source.loadList(), source.getPosition(), source.getPositionKey());
        }
    }

    public static boolean isListFile(String fileName) {
        return fileName.endsWith(".list");
    }

    public MediaWrapper next(int offset) {
        if (list == null || list.size() == 0) {
            return null;
        }
        currentPos = currentPos + offset;

        currentPos = ((currentPos % list.size()) + list.size()) % list.size();

        if (list.size() > 1) {
            listPosition.putInt(listPositionKey, currentPos);
        }

        return list.get(currentPos);

    }
}
