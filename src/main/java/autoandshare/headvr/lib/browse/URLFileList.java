package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;
import android.webkit.URLUtil;

import com.tencent.mmkv.MMKV;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class URLFileList implements PlayList.IPlayList {
    private String uriString;
    private boolean isReady = false;
    private String baseURL;
    private List<String> fileList;
    private int currentPos = -1;
    private MMKV listPosition;
    private Activity activity;

    private boolean hasPrefix(byte[] a, byte[] b) {
        for (int i = 0; i < b.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] BOM_UTF16_BE = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] BOM_UTF16_LE = {(byte) 0xFF, (byte) 0xFE};

    private Charset getCharset(byte[] bytes) {
        if (hasPrefix(bytes, BOM_UTF8)) {
            return StandardCharsets.UTF_8;
        }
        if (hasPrefix(bytes, BOM_UTF16_LE)) {
            return StandardCharsets.UTF_16LE;
        }
        if (hasPrefix(bytes, BOM_UTF16_BE)) {
            return StandardCharsets.UTF_16BE;
        }
        return null;
    }

    private int skipBOM(byte[] bytes, Charset charset) {
        if ((charset == StandardCharsets.UTF_8) && (hasPrefix(bytes, BOM_UTF8))) {
            return 3;
        } else if ((charset == StandardCharsets.UTF_16BE) && hasPrefix(bytes, BOM_UTF16_BE)) {
            return 2;
        } else if ((charset == StandardCharsets.UTF_16LE) && hasPrefix(bytes, BOM_UTF16_LE)) {
            return 2;
        }
        return 0;
    }

    private String decodeBytes(byte[] buf, int length, Charset charset) {
        try {
            int offset = skipBOM(buf, charset);
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(buf, offset, length - offset));
            return charBuffer.toString();
        } catch (Exception ex) {
        }
        return null;
    }

    private String parseBaseURL(String uriString) {
        int end = uriString.lastIndexOf("/");
        int end2 = uriString.lastIndexOf("%2F"); // for encoded "/"
        if (end2 > end) {
            end = end2 + 2;
        }
        return uriString.substring(0, end + 1);
    }

    public URLFileList(Uri uri, Activity activity) {
        uriString = uri.toString();

        this.activity = activity;

        listPosition = MMKV.mmkvWithID("List-Position");
        currentPos = listPosition.getInt(uriString, 0);

        baseURL = parseBaseURL(uriString);

        // use thread to work around NetworkOnMainThreadException
        new Thread(() -> {
            loadList(uri);
        }).start();
    }

    private void loadList(Uri uri) {
        try {
            try (InputStream stream = uri.getScheme().equals("content") ?
                    activity.getContentResolver().openInputStream(uri) :
                    new URL(uriString).openConnection().getInputStream()) {
                byte[] buf = new byte[128 * 1024];
                int read;
                int total = 0;

                while ((read = stream.read(buf, total, buf.length - total)) != -1) {
                    total = total + read;
                    if (total == buf.length) {
                        break;
                    }
                }

                String fileContent;

                Charset charset = getCharset(buf);
                if (charset != null) {
                    fileContent = decodeBytes(buf, total, charset);
                } else {
                    fileContent = decodeBytes(buf, total, StandardCharsets.UTF_8);
                    if (fileContent == null) {
                        fileContent = decodeBytes(buf, total, StandardCharsets.UTF_16LE);
                    }
                    if (fileContent == null) {
                        fileContent = decodeBytes(buf, total, StandardCharsets.UTF_16BE);
                    }
                }
                if (fileContent == null) {
                    return;
                }

                processFileList(fileContent);

            }
        } catch (Exception ex) {
        }
        isReady = true;
    }

    private void processFileList(String fileContent) {
        List<String> files = new ArrayList<>();
        Scanner scanner = new Scanner(fileContent);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("#") || (line.length() == 0)) {
                continue;
            }
            files.add(line.replace("\\", "/"));
        }
        if (files.size() != 0) {
            fileList = files;
        }
    }

    public Uri next(int offset) {
        currentPos = currentPos + offset;
        return current();
    }

    @Override
    public String currentIndex() {
        if ((!isReady) || (fileList == null)) {
            return "";
        }
        return "" + (currentPos + 1) + "/" + fileList.size() + " ";
    }

    public Uri prev(Uri uri) {
        return next(-1);
    }

    public Uri next(Uri uri) {
        return next(1);
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public Uri current() {
        if (fileList == null) {
            return null;
        }
        currentPos = ((currentPos % fileList.size()) + fileList.size()) % fileList.size();

        listPosition.putInt(uriString, currentPos);

        String fileName = fileList.get(currentPos);
        if (URLUtil.isValidUrl(fileName)) {
            return Uri.parse(fileName);
        } else {
            return Uri.parse(baseURL + Uri.encode(fileName));
        }
    }
}
