import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Adventurous {

    /* ===================== WINDOW ===================== */
    private long window;
    private int width = 1280, height = 720;
    private boolean paused = false;

    /* ===================== CAMERA ===================== */
    private volatile float camX = 0f;
    private volatile float camY = 3f;
    private volatile float camZ = -6f;
    private final float camSpeed = 0.08f;

    /* ===================== WORLD ===================== */
    private static float screenOffsetX, screenOffsetY;
    private final CopyOnWriteArrayList<IsoCube> cubes = new CopyOnWriteArrayList<>();

    /* ===================== TEXTURES ===================== */
    private int grassTexture;

    /* ===================== MAIN ===================== */
    public static void main(String[] args) {
        new Adventurous().run();
    }

    private void run() {
        init();
        startThreads();
        renderLoop();
        cleanup();
    }

    /* ===================== INIT ===================== */
    private void init() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Adventurous", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glfwShowWindow(window);

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(window, w, h);
        width = w[0];
        height = h[0];

        setup2D();
        updateScreenOffset();

        glEnable(GL_TEXTURE_2D);
        grassTexture = loadTexture("textures\\grassblock.png");

        createWorld();

        glfwSetFramebufferSizeCallback(window, (win, nw, nh) -> {
            width = nw;
            height = nh;
            glViewport(0, 0, width, height);
            setup2D();
            updateScreenOffset();
        });

        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                paused = !paused;
                glfwSetInputMode(window, GLFW_CURSOR,
                        paused ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (paused || action != GLFW_PRESS) return;
            IsoCube sel = getSelectedCube();
            if (sel == null) return;

            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                cubes.remove(sel);
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                cubes.add(new IsoCube(sel.x, sel.y + 1, sel.z, sel.size, grassTexture));
            }
        });

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    /* ===================== THREADS ===================== */
    private void startThreads() {

        // LOGIC THREAD
        new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                if (!paused) handleCameraMovement();
                sleep(5);
            }
        }, "LogicThread").start();

        // ASYNC THREAD (future gen/AI)
        new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                sleep(20);
            }
        }, "AsyncThread").start();

        // NETWORK / FUTURE
        new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                sleep(50);
            }
        }, "NetThread").start();
    }

    /* ===================== CAMERA ===================== */
    private void handleCameraMovement() {
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) camZ += camSpeed;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) camZ -= camSpeed;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) camX -= camSpeed;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) camX += camSpeed;
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) camY -= camSpeed;
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) camY += camSpeed;
    }

    /* ===================== RENDER LOOP ===================== */
    private void renderLoop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            drawSky();

            cubes.forEach(c -> c.updateScreenPos(camX, camY, camZ));
            cubes.sort(Comparator.comparingInt(c -> c.x + c.y + c.z));

            IsoCube selected = paused ? null : getSelectedCube();

            for (IsoCube c : cubes) {
                c.draw();
                if (c == selected) c.drawOutline();
            }

            drawCrosshair();
            drawHand();

            if (paused) drawPauseOverlay();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /* ===================== DRAWING ===================== */
    private void drawSky() {
        glDisable(GL_TEXTURE_2D);
        glColor3f(0.52f, 0.80f, 0.92f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(width, 0);
        glVertex2f(width, height);
        glVertex2f(0, height);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private void drawCrosshair() {
        if (paused) return;
        float cx = width / 2f, cy = height / 2f;
        glDisable(GL_TEXTURE_2D);
        glColor3f(1, 1, 1);
        glBegin(GL_LINES);
        glVertex2f(cx - 8, cy); glVertex2f(cx + 8, cy);
        glVertex2f(cx, cy - 8); glVertex2f(cx, cy + 8);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private void drawHand() {
        glDisable(GL_TEXTURE_2D);
        glColor3f(0.9f, 0.7f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(width - 90, height - 40);
        glVertex2f(width - 40, height - 40);
        glVertex2f(width - 40, height);
        glVertex2f(width - 90, height);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private void drawPauseOverlay() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0, 0, 0, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(width, 0);
        glVertex2f(width, height);
        glVertex2f(0, height);
        glEnd();
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    /* ===================== SELECTION ===================== */
    private IsoCube getSelectedCube() {
        float cx = width / 2f, cy = height / 2f;
        IsoCube best = null;
        double bestDist = Double.MAX_VALUE;

        for (IsoCube c : cubes) {
            if (c.contains(cx, cy)) {
                double d = c.distanceTo(cx, cy);
                if (d < bestDist) {
                    best = c;
                    bestDist = d;
                }
            }
        }
        return best;
    }

    /* ===================== WORLD ===================== */
    private void createWorld() {
        int size = 64;
        cubes.add(new IsoCube(0, 0, 0, size, grassTexture));
        cubes.add(new IsoCube(1, 0, 0, size, grassTexture));
        cubes.add(new IsoCube(0, 0, 1, size, grassTexture));
    }

    /* ===================== ISO CUBE ===================== */
    static class IsoCube {
        int x, y, z, size, tex;
        float sx, sy, hw, hh;
        float[] top, left, right;

        IsoCube(int x, int y, int z, int size, int tex) {
            this.x = x; this.y = y; this.z = z;
            this.size = size;
            this.tex = tex;
        }

        void updateScreenPos(float cx, float cy, float cz) {
            hw = size / 2f;
            hh = size / 4f;

            float rx = x - cx;
            float ry = y - cy;
            float rz = z - cz;

            sx = (rx - rz) * hw + screenOffsetX;
            sy = (rx + rz) * hh - ry * hh + screenOffsetY;

            top = new float[]{sx, sy - hh, sx + hw, sy, sx, sy + hh, sx - hw, sy};
            left = new float[]{sx - hw, sy, sx, sy + hh, sx, sy + hh + size / 2f, sx - hw, sy + size / 2f};
            right = new float[]{sx + hw, sy, sx, sy + hh, sx, sy + hh + size / 2f, sx + hw, sy + size / 2f};
        }

        void draw() {
            glBindTexture(GL_TEXTURE_2D, tex);
            drawFace(top);
            drawFace(left);
            drawFace(right);
        }

        void drawOutline() {
            glDisable(GL_TEXTURE_2D);
            glColor3f(1, 1, 0);
            drawLine(top);
            drawLine(left);
            drawLine(right);
            glEnable(GL_TEXTURE_2D);
        }

        boolean contains(float px, float py) {
            return px > sx - hw && px < sx + hw && py > sy - hh && py < sy + size;
        }

        double distanceTo(float px, float py) {
            return Math.hypot(px - sx, py - sy);
        }

        private void drawFace(float[] f) {
            glBegin(GL_QUADS);
            glTexCoord2f(0,0); glVertex2f(f[0], f[1]);
            glTexCoord2f(1,0); glVertex2f(f[2], f[3]);
            glTexCoord2f(1,1); glVertex2f(f[4], f[5]);
            glTexCoord2f(0,1); glVertex2f(f[6], f[7]);
            glEnd();
        }

        private void drawLine(float[] f) {
            glBegin(GL_LINE_LOOP);
            for (int i = 0; i < 8; i += 2) glVertex2f(f[i], f[i + 1]);
            glEnd();
        }
    }

    /* ===================== UTILS ===================== */
    private void setup2D() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    private void updateScreenOffset() {
        screenOffsetX = width / 2f;
        screenOffsetY = height / 2f;
    }

    private int loadTexture(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer img = STBImage.stbi_load(path, w, h, c, 4);
            if (img == null) throw new RuntimeException("Texture load failed");

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(), h.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, img);
            STBImage.stbi_image_free(img);
            return id;
        }
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
