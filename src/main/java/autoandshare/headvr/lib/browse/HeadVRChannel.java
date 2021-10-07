package autoandshare.headvr.lib.browse;

import android.app.Activity;

import com.tencent.mmkv.MMKV;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HeadVRChannel implements IPlayList {
    public static void setChannelURL(boolean forTest) {
        CHANNEL_URL = forTest ?
                "https://headvr.appspot.com/v1/test" :
                "https://headvr.appspot.com/v1/next";
    }

    private static String CHANNEL_URL;

    private static MMKV prefLast = MMKV.mmkvWithID("headVR-lasturl");

    private OkHttpClient client;
    private boolean justStarted = true;

    @Override
    public void setActivity(Activity activity) {
        CookieJar cookieJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };
        client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
    }

    private List<PlayItem> list = new ArrayList<PlayItem>();
    private int index = 0;

    public String getIndexString() {
        return "... " + (index + 1) + " ...";
    }

    @Override
    public PlayItem getMediaAtOffset(int offset) {
        if (list.size() == 0) {
            list.add(getNextPlayItem());
        }

        int newIndex = index + offset;

        if (newIndex < 0) {
            newIndex = list.size() - 1;
        }

        if (newIndex >= list.size()) {
            list.add(getNextPlayItem());
            newIndex = list.size() - 1;
        }

        index = newIndex;
        return list.get(index);
    }

    private PlayItem getNextPlayItem() {
        PlayItem item = null;

        if (justStarted) {
            justStarted = false;

            String lastJsonString = prefLast.getString("last", null);
            if (lastJsonString != null) {
                try {
                    item = getItemFromJsonString(lastJsonString);
                    return item;
                }
                catch (Exception ex) {
                }
            }
        }

        item = fetchPlayItem();
        return item;
    }

    private String fullUrl(String url) {
        if (url.toLowerCase().startsWith("https:")) {
            return url;
        }
        return "https://www.youtube.com/watch?app=desktop&v=" + url;
    }

    private PlayItem getItemFromJsonString(String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);

        PlayItem item = new PlayItem(fullUrl(obj.getString("URL")));

        item.layoutInfo = obj.optString("Layout");

        return item;
    }
    @Nullable
    private PlayItem fetchPlayItem() {
        Request request = new Request.Builder().url(CHANNEL_URL).build();
        PlayItem item = null;

        try (Response response = client.newCall(request).execute()) {

            String jsonString = response.body().string();
            item = getItemFromJsonString(jsonString);
            prefLast.putString("last", jsonString);

        } catch (Exception ex) {
        }
        return item;
    }
}
