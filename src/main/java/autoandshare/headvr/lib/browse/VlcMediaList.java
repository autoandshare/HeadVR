package autoandshare.headvr.lib.browse;

import android.net.Uri;
import android.util.Log;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.interfaces.IMediaList;
import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.util.ArrayList;
import java.util.List;

import autoandshare.headvr.BuildConfig;
import autoandshare.headvr.activity.VlcHelper;
import autoandshare.headvr.lib.PathUtil;

public class VlcMediaList implements PlayList.ListSource {

    private static String tag = "VlcMediaList";


    private VlcHelper.VlcSelection selection;
    private String listPositionKey;

    public VlcMediaList(VlcHelper.VlcSelection vlcSelection) {
        this.selection = vlcSelection;
    }

    public int getPosition() {
        return selection.position;
    }

    public String getPositionKey() {
        return listPositionKey;
    }

    public List<MediaWrapper> loadList() {
        List<MediaWrapper> list = new ArrayList<>();

        if (selection.list != null) {
            listPositionKey = PathUtil.getKey(Uri.parse(selection.listUrl));
            return selection.list;

        } else if (selection.mw != null) {

            listPositionKey = PathUtil.getKey(selection.mw.getUri());

            Media m = new Media(VlcHelper.Instance, selection.mw.getUri());

            expand(m, list);

            if (list.size() == 0) {
                list.add(selection.mw);
            }
        }
        return list;
    }

    private void expand(IMedia m, List<MediaWrapper> list) {
        m.parse(Media.Parse.ParseNetwork);

        IMediaList ml = m.subItems();
        if (ml != null) {
            for (int i = 0; i < ml.getCount(); i++) {
                IMedia sub_m = ml.getMediaAt(i);
                if ((sub_m.getType() == Media.Type.Directory) || (sub_m.getType() == Media.Type.Playlist)) {
                    expand(sub_m, list);
                } else {
                    MediaWrapper mw = MLServiceLocator.getAbstractMediaWrapper(sub_m);
                    if (mw != null) {
                        if ((mw.getType() == MediaWrapper.TYPE_VIDEO) || mw.getUri().getScheme().startsWith("http")) {
                            list.add(mw);
                        }
                    }
                    sub_m.release();
                }
            }
            ml.release();
        }

        m.release();
    }
}
