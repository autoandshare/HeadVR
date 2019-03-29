package autoandshare.headvr.lib.browse;

import android.net.Uri;

import org.videolan.medialibrary.media.MediaWrapper;

import java.util.List;

public class VlcMediaList implements PlayList.IPlayList {

    private List<MediaWrapper> list;
    private int currentPos = 0;
    public VlcMediaList(List<MediaWrapper> list) {
        this.list = list;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public Uri current() {
        return list.get(currentPos).getUri();
    }

    @Override
    public Uri next(int offset) {
        currentPos = (currentPos + offset) % list.size();
        return current();
    }

    @Override
    public String currentIndex() {
        if (list == null) {
            return "";
        }
        return "" + (currentPos + 1) + "/" + list.size() + " ";
    }
}
