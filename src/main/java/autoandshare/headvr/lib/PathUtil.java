package autoandshare.headvr.lib;

import android.net.Uri;

public class PathUtil {
    public static String getFilename(Uri uri) {
        String[] path = uri.getPath().split("/");
        return path[path.length - 1];

    }

    public static String getKey(Uri uri) {
        // for file, name is enough
        if ("file,ftp,smb,ftps,sftp,nfs".contains(uri.getScheme())) {
            return getFilename(uri);
        }
        return uri.toString();
    }

}
