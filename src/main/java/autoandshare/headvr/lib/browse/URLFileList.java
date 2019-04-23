package autoandshare.headvr.lib.browse;

import android.app.Activity;
import android.net.Uri;
import android.webkit.URLUtil;

import org.videolan.medialibrary.media.MediaWrapper;

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

import autoandshare.headvr.lib.PathUtil;

public class URLFileList implements PlayList.ListSource {
    private Uri uri;
    private Activity activity;

    public URLFileList(Uri uri, Activity activity) {
        this.uri = uri;
        this.activity = activity;
    }

    public int getPosition() {
        return -1;
    }

    public String getPositionKey() {
        return PathUtil.getKey(uri);
    }

    public List<MediaWrapper> loadList() {
        return processFileList(readContent(), parseBaseURL(uri.toString()));
    }

    private String readContent() {
        String fileContent = null;

        try (InputStream stream = uri.getScheme().equals("content") ?
                activity.getContentResolver().openInputStream(uri) :
                new URL(uri.toString()).openConnection().getInputStream()) {
            byte[] buf = new byte[2 * 1024 * 1024];
            int read;
            int total = 0;

            while ((read = stream.read(buf, total, buf.length - total)) != -1) {
                total = total + read;
                if (total == buf.length) {
                    break;
                }
            }

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

        } catch (Exception ex) {
        }

        return fileContent;
    }

    private MediaWrapper getMediaWrapper(String line, String baseURL) {
        return new MediaWrapper(URLUtil.isValidUrl(line) ?
                Uri.parse(line) :
                Uri.parse(baseURL + Uri.encode(line)));
    }

    private List<MediaWrapper> processFileList(String fileContent, String baseURL) {
        if (fileContent == null) {
            return null;
        }

        List<MediaWrapper> list = new ArrayList<>();

        Scanner scanner = new Scanner(fileContent);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.startsWith("#") || (line.length() == 0)) {
                continue;
            }
            line = line.replace("\\", "/");
            list.add(getMediaWrapper(line, baseURL));
        }

        return list;
    }


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

}
