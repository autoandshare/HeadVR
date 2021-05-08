package autoandshare.headvr.lib;

import android.net.Uri;

public class PathUtil {
    public static String getFilename(String path) {
        String[] parts = path.split("/");
        if ((parts != null) && (parts.length != 0)) {
            return parts[parts.length - 1];
        } else {
            return "";
        }
    }

    public static String getFilename(Uri uri) {
        return getFilename(uri.getPath());
    }

    public static boolean isFileAccessProtocol(String scheme) {
        return "file,ftp,smb,ftps,sftp,nfs".contains(scheme.toLowerCase());
    }

    public static String getKey(Uri uri) {
        // for file, name is enough
        if (isFileAccessProtocol(uri.getScheme())) {
            return getFilename(uri);
        }
        return uri.toString();
    }

}
