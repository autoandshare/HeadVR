package autoandshare.headvr.lib.browse;

import android.app.Activity;

import org.videolan.medialibrary.interfaces.media.MediaWrapper;

public interface IPlayList {
    public void setActivity(Activity activity);
    public String getIndexString();
    public MediaWrapper getMediaAtOffset(int offset);
}
