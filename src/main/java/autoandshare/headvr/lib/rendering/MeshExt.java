package autoandshare.headvr.lib.rendering;

import java.nio.FloatBuffer;

public class MeshExt extends Mesh {
    /**
     * A spherical mesh for video should be large enough that there are no stereo artifacts.
     */
    private static final int SPHERE_RADIUS_METERS = 50;

    /**
     * These should be configured based on the video type. But this sample assumes 360 video.
     */
    private static final int DEFAULT_SPHERE_VERTICAL_DEGREES = 180;
    private static final int DEFAULT_SPHERE_HORIZONTAL_DEGREES = 360;

    /**
     * The 360 x 180 sphere has 15 degree quads. Increase these if lines in your video look wavy.
     */
    private static final int DEFAULT_SPHERE_ROWS = 12;
    private static final int DEFAULT_SPHERE_COLUMNS = 24;

    protected static FloatBuffer getDefaultVertexBuffer(int horizontalDegrees, int mediaFormat) {
        float[] vertexData = getVertexData(
                SPHERE_RADIUS_METERS, DEFAULT_SPHERE_ROWS, DEFAULT_SPHERE_COLUMNS,
                DEFAULT_SPHERE_VERTICAL_DEGREES, horizontalDegrees,
                mediaFormat);
        defaultLength = vertexData.length;
        return Utils.createBuffer(vertexData);
    }

    private static int defaultLength;
    private static FloatBuffer[] vertexBuffer360 = new FloatBuffer[3];
    private static FloatBuffer[] vertexBuffer180 = new FloatBuffer[3];

    static {
        for (int i = 0; i < 3; i++) {
            vertexBuffer360[i] = getDefaultVertexBuffer(360, i);
            vertexBuffer180[i] = getDefaultVertexBuffer(180, i);
        }
    }

    public void setMediaType(boolean vr180, int mediaFormat) {
        if (vr180) {
            this.vertexBuffer = vertexBuffer180[mediaFormat];
        } else {
            this.vertexBuffer = vertexBuffer360[mediaFormat];
        }

    }

    public MeshExt() {
        this.verticesLength = defaultLength;
        this.vertexBuffer = vertexBuffer360[0];
        return;
    }

}

