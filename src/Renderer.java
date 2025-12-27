import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    public void init() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspect = 1280f / 720f;
        glFrustum(-aspect, aspect, -1, 1, 1, 1000);
        glMatrixMode(GL_MODELVIEW);
    }

    public void beginFrame(Camera cam) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glLoadIdentity();
        cam.applyView();
    }

    public void endFrame() {
        // reserved for post-processing later
    }
}
