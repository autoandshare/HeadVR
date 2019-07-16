package autoandshare.headvr.lib.rendering;

import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.vr.sdk.base.Eye;

import java.nio.FloatBuffer;

import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.VideoRenderer;

import static autoandshare.headvr.lib.rendering.Utils.checkGlError;

public class VRTexture2D {
    private static FloatBuffer[] textureBufferLeft = new FloatBuffer[3];
    private static FloatBuffer[] textureBufferRight = new FloatBuffer[3];

    static {
        textureBufferLeft[0] = createTextureCoordsBuffers(new PointF(0, 1), new PointF(1, 0));
        textureBufferRight[0] = textureBufferLeft[0];

        textureBufferLeft[1] = createTextureCoordsBuffers(new PointF(0, 1), new PointF(0.5f, 0));
        textureBufferRight[1] = createTextureCoordsBuffers(new PointF(0.5f, 1), new PointF(1, 0));

        textureBufferLeft[2] = createTextureCoordsBuffers(new PointF(0, 1), new PointF(1, 0.5f));
        textureBufferRight[2] = createTextureCoordsBuffers(new PointF(0, 0.5f), new PointF(1, 0));
    }

    public boolean verticalFixed = false;

    private int mediaType;

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    // constructors
    public VRTexture2D() {
        createSurfaceTexture();
    }

    public void updatePositions(float width, float height, float distance, PointF topLeft) {
        createVertexCoordsBuffer(width, height, distance, topLeft);
    }

    // gl resources
    private static final String[] vertexShaderCode = {
            "precision mediump float;",
            "attribute vec4 a_Position;",
            "attribute vec4 a_TextureCoordinate;",
            "uniform mat4 u_TextureTransform;",
            "uniform mat4 u_MVP;",
            "varying vec2 v_TextureCoordinate;",
            "void main() {",
            "    v_TextureCoordinate = (u_TextureTransform * a_TextureCoordinate).xy;",
            "    gl_Position = u_MVP * a_Position;",
            "}",
    };
    private static final String[] fragmentShaderCode = {
            "#extension GL_OES_EGL_image_external : require",
            "precision mediump float;",
            "uniform samplerExternalOES u_Texture;",
            "varying vec2 v_TextureCoordinate;",
            "void main () {",
            "    gl_FragColor = texture2D(u_Texture, v_TextureCoordinate);",
            "}",
    };
    private static int shaderProgram;

    private static int textureCoordsParam;
    private static int vertexCoordsParam;
    private static int textureTranformParam;
    private static int MVPParam;
    private static int textureHandle;

    public static void glInit() {

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        shaderProgram = Utils.compileProgram(vertexShaderCode, fragmentShaderCode);

        textureCoordsParam = GLES20.glGetAttribLocation(shaderProgram, "a_TextureCoordinate");
        vertexCoordsParam = GLES20.glGetAttribLocation(shaderProgram, "a_Position");
        textureTranformParam = GLES20.glGetUniformLocation(shaderProgram, "u_TextureTransform");
        MVPParam = GLES20.glGetUniformLocation(shaderProgram, "u_MVP");
        textureHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Texture");

        checkGlError();

    }

    private FloatBuffer vertexCoordsBuffer;

    private void createVertexCoordsBuffer(float width, float height, float distance,
                                          PointF topLeft) {
        if (topLeft == null) {
            topLeft = new PointF(-width / 2, height / 2);
        }
        float[] vertexCoords = {
                topLeft.x, topLeft.y, -distance,
                topLeft.x + width, topLeft.y, -distance,
                topLeft.x, topLeft.y - height, -distance,
                topLeft.x + width, topLeft.y - height, -distance,
        };
        vertexCoordsBuffer = Utils.createBuffer(vertexCoords);

    }

    private static FloatBuffer createTextureCoordsBuffers(PointF textureTopLeft, PointF textureBottomRight
    ) {
        float[] textureCoords = {
                textureTopLeft.x, textureTopLeft.y,
                textureBottomRight.x, textureTopLeft.y,
                textureTopLeft.x, textureBottomRight.y,
                textureBottomRight.x, textureBottomRight.y,
        };
        return Utils.createBuffer(textureCoords);
    }

    // surface texture
    private int textureId;
    private SurfaceTexture surfaceTexture;

    private void createSurfaceTexture() {
        textureId = Utils.glCreateExternalTexture();
        surfaceTexture = new SurfaceTexture(textureId);
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public int getTextureId() {
        return textureId;
    }


    private float[] textureTransform = new float[16];

    public void draw(Eye eye) {
        surfaceTexture.getTransformMatrix(textureTransform);

        GLES20.glUseProgram(shaderProgram);
        checkGlError();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(textureHandle, 0);
        checkGlError();

        GLES20.glUniformMatrix4fv(textureTranformParam, 1, false, textureTransform, 0);
        GLES20.glUniformMatrix4fv(MVPParam, 1, false,
                getMVP(eye), 0);
        checkGlError();

        GLES20.glEnableVertexAttribArray(vertexCoordsParam);
        GLES20.glVertexAttribPointer(vertexCoordsParam, 3, GLES20.GL_FLOAT,
                false, 0, vertexCoordsBuffer);
        checkGlError();

        GLES20.glEnableVertexAttribArray(textureCoordsParam);
        checkGlError();
        GLES20.glVertexAttribPointer(textureCoordsParam, 2, GLES20.GL_FLOAT,
                false, 0,
                VideoRenderer.useRightTexture(eye.getType()) ?
                        textureBufferRight[mediaType] :
                        textureBufferLeft[mediaType]);
        checkGlError();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError();
    }


    private float[] mvp = new float[16];
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private float[] getMVP(Eye eye) {

        // use different distance for mono and stereo content
        float eyeDistance = VideoRenderer.getCurrentEyeDistance();

        Matrix.setIdentityM(mvp, 0);

        Matrix.translateM(mvp, 0,
                eye.getType() == 1 ? -eyeDistance : eyeDistance,
                verticalFixed ? 0 : Setting.VerticalDistance,
                0);

        float CAMERA_Z = 0.01f;
        float[] camera = new float[16];
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, -3f, 0.0f, 1.0f, 0.0f);

        // Build the ModelView and ModelViewProjection matrices
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        float[] videoMVP = new float[16];
        Matrix.multiplyMM(videoMVP, 0, camera, 0, mvp, 0);
        Matrix.multiplyMM(videoMVP, 0, perspective, 0, videoMVP, 0);

        return videoMVP;
    }
}
