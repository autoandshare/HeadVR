package autoandshare.headvr.lib.rendering;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.Surface;


public class VRSurface extends VRTexture2D {
    private Surface surface;

    public VRSurface() {
    }

    public void init(
            float width,
            float height, float distance,
            PointF topLeft, int widthPixel, int heightPixel) {

        this.verticalFixed = true;
        this.setMediaType(Mesh.MEDIA_MONOSCOPIC);
        this.updatePositions(
                width, height, distance,
                topLeft);

        this.getSurfaceTexture().setDefaultBufferSize(
                widthPixel, heightPixel);

        surface = new Surface(this.getSurfaceTexture());
    }

    public Canvas getCanvas() {
        return surface.lockCanvas(null);
    }

    public void releaseCanvas(Canvas canvas) {
        surface.unlockCanvasAndPost(canvas);
        this.getSurfaceTexture().updateTexImage();
    }
}
