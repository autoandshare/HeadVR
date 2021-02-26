package autoandshare.headvr.lib;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.HashMap;

public class Setting {
    public static float Brightness;
    public static float EyeDistance;
    public static float VerticalDistance;
    public static float VideoSize;
    public static float MotionSensitivity;

    public enum id {
        Brightness,
        EyeDistance,
        VerticalDistance,
        VideoSize,
        MotionSensitivity,
        DisableDistortionCorrection,
        HeadControl,
    }

    public static boolean DisableExtraFunction;

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
        items.put(id.EyeDistance, new Item(-50, 50, 0, -0.4f, 0.4f, "Eye to eye distance (near <-> far)"));
        items.put(id.VerticalDistance, new Item(-50, 50, 0, -0.6f, 0.6f, "Vertical offset (low <-> high)"));
        items.put(id.VideoSize, new Item(50, 150, 100, 1.5f, 6.5f, "Video size (small <-> big)"));
        items.put(id.MotionSensitivity, new Item(-10, 10, 0, 0.09f, 0.01f, "Head motion sensitivity (less <-> more)"));
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
        VerticalDistance = getFloat(id.VerticalDistance);
        VideoSize = getFloat(id.VideoSize);
        MotionSensitivity = getFloat(id.MotionSensitivity);
    }

    public boolean getBoolean(id name) {
        return pref.getBoolean(name.toString(), true);
    }

    public void putBoolean(id name, boolean value) {
        editor.putBoolean(name.toString(), value);
        editor.apply();
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

    public int update(id name, int delta) {
        int newValue = get(name) + delta;
        if (newValue > getMax(name)) {
            newValue = getMax(name);
        }
        if (newValue < getMin(name)) {
            newValue = getMin(name);
        }
        set(name, newValue);
        return newValue;
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
