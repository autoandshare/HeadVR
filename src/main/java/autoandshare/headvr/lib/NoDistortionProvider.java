package autoandshare.headvr.lib;

import android.content.Context;
import android.net.Uri;


import com.google.common.logging.nano.Vr.VREvent.SdkConfigurationParams;
import com.google.vr.sdk.proto.nano.CardboardDevice.DeviceParams;
import com.google.vr.sdk.proto.nano.CardboardDevice.DeviceParamsList;
import com.google.vr.sdk.proto.nano.Display.DisplayParams;
import com.google.vr.sdk.proto.nano.Preferences.UserPrefs;
import com.google.vr.sdk.proto.nano.SdkConfiguration.SdkConfigurationRequest;

import com.google.vr.cardboard.ConfigUtils;
import com.google.vr.cardboard.LegacyVrParamsProvider;
import com.google.vr.cardboard.VrParamsProvider;
import com.google.vr.cardboard.VrParamsProviderFactory;

import java.lang.reflect.Field;
/*
 * There is some bug in google vr. When distortion correction is disabled, some phones
 * have render issue.
 */
public final class NoDistortionProvider implements VrParamsProvider {
    public static void setupProvider(Context context) {
        try {
            Field field = VrParamsProviderFactory.class.getDeclaredField("providerForTesting");
            field.setAccessible(true);
            field.set(null, new NoDistortionProvider(context));
        } catch (Exception ex) {
        }
    }

    private static final String TAG = NoDistortionProvider.class.getSimpleName();
    private final LegacyVrParamsProvider legacyVrParamsProvider;

    public NoDistortionProvider(Context context) {
        this.legacyVrParamsProvider = new LegacyVrParamsProvider(context);
    }

    public final DeviceParams readDeviceParams() {

        return ConfigUtils.readDeviceParamsFromUri(
                Uri.parse("http://google.com/cardboard/cfg?p=CgRUZXN0EgRUZXN0HTEILD0lj8J1PSoQAABIQgAASEIAAEhCAABIQlgANSlcDz06CG8SgzpvEoM6UABgAA"));
    }

    public final boolean writeDeviceParams(DeviceParams deviceParams) {
        return false;
    }

    public final DisplayParams readDisplayParams() {
        return legacyVrParamsProvider.readDisplayParams();
    }

    public final UserPrefs readUserPrefs() {
        return null;
    }

    public final boolean updateUserPrefs(UserPrefs userPrefs) {
        return false;
    }

    public final SdkConfigurationParams readSdkConfigurationParams(SdkConfigurationRequest sdkConfigurationRequest) {
        return null;
    }

    public final void close() {
    }

    public final DeviceParamsList readRecentHeadsets() {
        return legacyVrParamsProvider.readRecentHeadsets();
    }
}
