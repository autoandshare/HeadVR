package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;

import org.videolan.medialibrary.media.MediaWrapper;

import java.util.List;
import java.util.regex.Pattern;

public class PlayList {

    public static IPlayList getPlayList(List<MediaWrapper> mediaWrapperList) {
        return new VlcMediaList(mediaWrapperList);
    }

    public interface IPlayList {
        public boolean isReady();

        public Uri current();

        public Uri next(int offset);

        public String currentIndex();
    }

    public static IPlayList getPlayList(Uri uri, Activity activity) {
        if (isListFile(uri.getPath())) {
            return new URLFileList(uri, activity);
        } else if (isLocalFile(uri)) {
            return new LocalFileList(uri);
        }
        return null;
    }

    private static Pattern fileNamePattern =
            Pattern.compile("[.](3gp|mkv|mp4|ts|webm|asf|wmv|avi|flv|mov|mpg|rmvb|ogg|ts)$",
                    Pattern.CASE_INSENSITIVE);

    public static boolean isKnownExtension(String fileName) {
        return fileNamePattern.matcher(fileName).find();
    }

    public static boolean isListFile(String fileName) {
        return fileName.endsWith(".list");
    }

    public static boolean isLocalFile(Uri uri) {
        return uri.getScheme().toLowerCase().equals("file");
    }
}
