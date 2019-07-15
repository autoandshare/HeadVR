package autoandshare.headvr.lib;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.HashMap;

public class Setting {
    public static float Brightness;
    public static float EyeDistance;
    public static float EyeDistance3D;
    public static float VerticalDistance;
    public static float VideoSize;
    public static float MotionSensitivity;


    public enum id {
        Brightness,
        EyeDistance,
        EyeDistance3D,
        VerticalDistance,
        VideoSize,
        MotionSensitivity,
    }

    private static class Item {
        int min;
        int max;
        int defaultValue;
        float minF;
        float maxF;
        String description;

        Item(int min, int max, int defaultValue, float minF, float maxF, String description) {
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.minF = minF;
            this.maxF = maxF;
            this.description = description;
        }
    }

    private static HashMap<id, Item> items;

    static {
        items = new HashMap<>();
        items.put(id.Brightness, new Item(0, 100, 25, 0f, 0.5f, "Brightness"));
        items.put(id.EyeDistance, new Item(-50, 50, 0, -0.4f, 0.4f, "Eye to eye distance"));
        items.put(id.EyeDistance3D, new Item(-50, 50, 0, -0.4f, 0.4f, "Eye to eye distance for 3D content"));
        items.put(id.VerticalDistance, new Item(-50, 50, 0, -0.6f, 0.6f, "Vertical offset"));
        items.put(id.VideoSize, new Item(50, 150, 100, 1.5f, 6.5f, "Video size"));
        items.put(id.MotionSensitivity, new Item(-10, 10, 0, 0.09f, 0.01f, "Head motion sensitivity"));
    }

    private float getFloat(id name) {
        Item item = items.get(name);
        return item.minF + (item.maxF - item.minF) * (get(name) - item.min) / (item.max - item.min);
    }

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public void clear() {
        editor.clear().commit();
    }

    private void loadValues() {
        Brightness = getFloat(id.Brightness);
        EyeDistance = getFloat(id.EyeDistance);
        EyeDistance3D = getFloat(id.EyeDistance3D);
        VerticalDistance = getFloat(id.VerticalDistance);
        VideoSize = getFloat(id.VideoSize);
        MotionSensitivity = getFloat(id.MotionSensitivity);
    }

    public Setting(Activity activity) {
        pref = activity.getSharedPreferences("Setting", 0);
        editor = pref.edit();
        loadValues();
    }

    public int getMin(id name) {
        return items.get(name).min;
    }

    public int getMax(id name) {
        return items.get(name).max;
    }

    public int get(id name) {
        return pref.getInt(name.toString(), items.get(name).defaultValue);
    }

    public String getDescription(id name) {
        return items.get(name).description;
    }

    public void set(id name, int value) {
        editor.putInt(name.toString(), value);
        editor.apply();

        switch (name) {
            case Brightness:
                Brightness = getFloat(name);
                break;
            case EyeDistance:
                EyeDistance = getFloat(name);
                break;
            case EyeDistance3D:
                EyeDistance3D = getFloat(name);
                break;
            case VerticalDistance:
                VerticalDistance = getFloat(name);
                break;
            case VideoSize:
                VideoSize = getFloat(name);
                break;
            case MotionSensitivity:
                MotionSensitivity = getFloat(name);
                break;
        }
    }
}
