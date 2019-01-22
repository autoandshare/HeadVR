package autoandshare.headvr.lib.browse;

import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalFileList implements PlayList.IPlayList {
    private int currentPos = -1;
    private List<File> fileList = null;

    private void processDir(File dir, File currentFile) {
        fileList = new ArrayList<>();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.equals(currentFile)) {
                    currentPos = fileList.size();
                    fileList.add(file);
                } else {
                    if (PlayList.isKnownExtension(file.getPath())) {
                        fileList.add(file);
                    }
                }
            }
        }
        if (currentPos == -1) {
            currentPos = fileList.size();
            fileList.add(currentFile);
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

        File currentFile = new File(uri.getPath());
        File dir = currentFile.getParentFile();
        if (dir == null) {
            return;
        }
        processDir(dir, currentFile);
    }

    @Override
    public Uri next(int offset) {
        currentPos = (currentPos + offset);
        return current();
    }

    @Override
    public Uri current() {
        if (fileList == null) {
            return null;
        }
        currentPos = ((currentPos % fileList.size()) + fileList.size()) % fileList.size();
        return Uri.fromFile(fileList.get(currentPos));
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
