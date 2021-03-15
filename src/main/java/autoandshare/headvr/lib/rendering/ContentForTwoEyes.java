package autoandshare.headvr.lib.rendering;

import com.google.vr.sdk.base.Eye;

public class ContentForTwoEyes {
    public static boolean force2D;
    public static float EyeDistance;
    public static float EyeDistance3D;
    public static float VerticalDistance;
    public static float VideoSize;
    protected int mediaType;

    public float getEyeDistance() {
        return mediaType == Mesh.MEDIA_MONOSCOPIC || force2D ?
                EyeDistance : EyeDistance3D;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    protected boolean useRightTexture(int eyeType) {
        return eyeType == Eye.Type.RIGHT || force2D;
    }
}
