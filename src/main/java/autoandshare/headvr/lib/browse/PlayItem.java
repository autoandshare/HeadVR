package autoandshare.headvr.lib.browse;

import android.net.Uri;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

public class PlayItem {
    public MediaWrapper mw;
    public String layoutInfo;

    public PlayItem(MediaWrapper mw) {
        this.mw = mw;
    }

    public PlayItem(String url) {
        this.mw = MLServiceLocator.getAbstractMediaWrapper(Uri.parse(url));
    }

}
