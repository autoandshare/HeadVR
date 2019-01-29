package autoandshare.headvr.lib.browse;

import android.net.Uri;

import java.util.regex.Pattern;

public class PlayList {
    public interface IPlayList {
        public boolean isReady();

        public Uri current();

        public Uri next(int offset);

        public String currentIndex();
    }

    public static IPlayList getPlayList(Uri uri) {
        if (isListFile(uri.getPath())) {
            return new URLFileList(uri);
        } else if (isLocalFile(uri)) {
            return new LocalFileList(uri);
        }
        return null;
    }

    private static Pattern fileNamePattern =
            Pattern.compile("[.](3gp|mkv|mp4|ts|webm)$",
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
