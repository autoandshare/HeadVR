package autoandshare.headvr.lib.browse;

import android.net.Uri;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;

public class LocalFileList {
    private File[] fileList = null;

    public LocalFileList(Uri uri) {
        if (!uri.toString().startsWith("file://")) {
            return;
        }
        File dir = new File(uri.getPath()).getParentFile();
        if (dir == null) {
            return;
        }
        fileList = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });
    }

    private int find(Uri uri) {
        if (fileList == null) {
            return -1;
        }

        String path = uri.getPath();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].getPath().equals(path)) {
                return i;
            }
        }

        return -1;
    }

    private Uri next(Uri uri, int offset) {
        int pos = find(uri);
        if (pos >= 0) {
            int newPos = pos + offset;
            if ((newPos >= 0) && (newPos < fileList.length)) {
                return Uri.fromFile(fileList[newPos]);
            }
        }
        return null;
    }

    public Uri next(Uri uri) {
        return next(uri, 1);
    }

    public Uri previous(Uri uri) {
        return next(uri, -1);
    }

}
