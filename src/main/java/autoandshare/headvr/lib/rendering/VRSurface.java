package autoandshare.headvr.lib.rendering;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.Surface;

import com.google.vr.sdk.base.Eye;

public class VRSurface {
    private VRTexture2D vrTexture2D;
    private Surface surface;

    public VRSurface(float width, float height, float distance,
                     PointF topLeft, int widthPixel, int heightPixel) {
        vrTexture2D = new VRTexture2D();
        vrTexture2D.verticalFixed = true;
        vrTexture2D.setMediaType(Mesh.MEDIA_MONOSCOPIC);
        vrTexture2D.updatePositions(
                width, height, distance,
                topLeft);

        vrTexture2D.getSurfaceTexture().setDefaultBufferSize(
                widthPixel, heightPixel);

        surface = new Surface(vrTexture2D.getSurfaceTexture());
    }

    public Canvas getCanvas() {
        return surface.lockCanvas(null);
    }

    public void releaseCanvas(Canvas canvas) {
        surface.unlockCanvasAndPost(canvas);
        vrTexture2D.getSurfaceTexture().updateTexImage();
    }

    public void draw(Eye eye) {
        vrTexture2D.draw(eye);
    }
}
