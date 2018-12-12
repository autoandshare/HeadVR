package autoandshare.headvr.lib.browse;

import android.net.Uri;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocalFileList {
    private List<File> fileList = new ArrayList<>();

    private final String[] videoExtension = {".3gp", ".mp4", ".ts", ".webm", ".mkv"};
    private boolean filterExtension = false;

    private boolean isKnownExtension(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        for (String ext : videoExtension) {
            if (lowerCaseFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private int count = 0; // limit to 300 files
    private int maxCount = 300;

    private void processDir(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                count += 1;
                if (count > maxCount) {
                    break;
                }
                if (((!filterExtension) || isKnownExtension(file.getPath()))) {
                    fileList.add(file);
                }
            }
        }
        for (File file : files) {
            if (file.isDirectory()) {
                count += 1;
                if (count > maxCount) {
                    break;
                }
                processDir(file);
            }
        }
    }

    public LocalFileList(Uri uri) {
        if (uri == null) {
            return;
        }

        String uriString = uri.toString();
        if (!uriString.startsWith("file://")) {
            return;
        }
        filterExtension = isKnownExtension(uriString);

        File dir = new File(uri.getPath()).getParentFile();
        if (dir == null) {
            return;
        }
        processDir(dir);
    }

    private int find(String path) {
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).getPath().equals(path)) {
                return i;
            }
        }

        return -1;
    }

    private String lastPath;
    private int lastIdx;

    private Uri next(Uri uri, int offset) {
        String path = uri.getPath();
        int pos;
        if (path.equals(lastPath)) {
            pos = lastIdx;
        } else {
            pos = find(path);
        }
        if (pos >= 0) {
            int newPos = (pos + offset + fileList.size()) % fileList.size();

            lastPath = fileList.get(newPos).getPath();
            lastIdx = newPos;

            return Uri.fromFile(fileList.get(newPos));
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
