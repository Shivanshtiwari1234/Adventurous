import org.lwjgl.glfw.*;
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

    private long window;
    private int width = 800, height = 600;
    private boolean paused = false;

    private final CopyOnWriteArrayList<IsoCube> cubes = new CopyOnWriteArrayList<>();

    private static float screenOffsetX, screenOffsetY;

    private int grassTexture;

    public static void main(String[] args) {
        new Adventurous().run();
    }

    private void run() {
        init();
        startThreads();
        loop();
        cleanup();
    }

    private void init() {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Adventurous Isometric XYZ", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glfwShowWindow(window);

        int[] fbWidth = new int[1], fbHeight = new int[1];
        glfwGetFramebufferSize(window, fbWidth, fbHeight);
        width = fbWidth[0];
        height = fbHeight[0];

        setup2D(width, height);
        updateScreenOffset();

        glEnable(GL_TEXTURE_2D);
        grassTexture = loadTexture("grassblock.png");

        createInitialCubes();

        // Callbacks
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            if (w > 0 && h > 0) {
                width = w; height = h;
                glViewport(0, 0, width, height);
                setup2D(width, height);
                updateScreenOffset();
                cubes.forEach(IsoCube::updateScreenPos);
            }
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                paused = !paused;
                glfwSetInputMode(window, GLFW_CURSOR, paused ? GLFW_CURSOR_NORMAL : GLFW_CURSOR_HIDDEN);
                if (!paused) centerCursor();
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (!paused && action == GLFW_PRESS) {
                IsoCube selected = getSelectedCube();
                if (selected == null) return;
                if (button == GLFW_MOUSE_BUTTON_LEFT) removeCube(selected);
                else if (button == GLFW_MOUSE_BUTTON_RIGHT)
                    addCube(selected.x, selected.y + 1, selected.z);
            }
        });

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        centerCursor();
    }

    private void startThreads() {
        // Thread 1: Game logic updates
        Thread logicThread = new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                if (!paused) {
                    // Future logic here (movement, block updates)
                }
                try { Thread.sleep(5); } catch (InterruptedException ignored) {}
            }
        }, "GameLogicThread");
        logicThread.setDaemon(true);
        logicThread.start();

        // Thread 2: Background computations
        Thread asyncThread = new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                if (!paused) {
                    // Heavy calculations, AI, procedural generation, etc.
                }
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }, "AsyncComputationThread");
        asyncThread.setDaemon(true);
        asyncThread.start();

        // Thread 3: Future networking / additional updates
        Thread networkThread = new Thread(() -> {
            while (!glfwWindowShouldClose(window)) {
                // Placeholder for multiplayer / network sync
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }
        }, "NetworkThread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private void updateScreenOffset() {
        screenOffsetX = width / 2f;
        screenOffsetY = height / 2f;
    }

    private void centerCursor() {
        glfwSetCursorPos(window, width / 2.0, height / 2.0);
    }

    private void setup2D(int w, int h) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, w, h, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    private void createInitialCubes() {
        int size = Math.min(width, height) / 8;
        cubes.add(new IsoCube(0, 0, 0, size, grassTexture));
        cubes.add(new IsoCube(1, 0, 0, size, grassTexture));
        cubes.add(new IsoCube(0, 0, 1, size, grassTexture));
        cubes.add(new IsoCube(1, 0, 1, size, grassTexture));
        cubes.forEach(IsoCube::updateScreenPos);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);

            // Draw sky
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.53f, 0.81f, 0.92f);
            glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
            glEnd();
            glEnable(GL_TEXTURE_2D);

            // Draw cubes (sorted for correct overlap)
            cubes.sort(Comparator.comparingInt(c -> c.x + c.y + c.z));
            IsoCube selectedCube = !paused ? getSelectedCube() : null;
            cubes.forEach(cube -> {
                cube.draw();
                if (cube == selectedCube) cube.drawOutline();
            });

            // Cursor & hand
            glDisable(GL_TEXTURE_2D);
            if (!paused) drawCursor();
            drawHand();
            glEnable(GL_TEXTURE_2D);

            // Pause overlay
            if (paused) drawPauseOverlay();

            glfwSwapBuffers(window);
            glfwPollEvents();

            if (!paused) centerCursor();
        }
    }

    private void drawCursor() {
        float cx = width / 2f, cy = height / 2f, size = 10f;
        glColor3f(1f, 1f, 1f);
        glBegin(GL_LINES);
        glVertex2f(cx - size, cy);
        glVertex2f(cx + size, cy);
        glVertex2f(cx, cy - size);
        glVertex2f(cx, cy + size);
        glEnd();
    }

    private void drawHand() {
        float handW = 40, handH = 40, armW = 15, armH = 50;
        float x = width - handW - 20, y = height - handH - 20;

        glColor3f(0.9f, 0.7f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(x + handW - armW, y - armH);
        glVertex2f(x + handW, y - armH);
        glVertex2f(x + handW, y);
        glVertex2f(x + handW - armW, y);
        glEnd();

        glBegin(GL_QUADS);
        glVertex2f(x, y - handH);
        glVertex2f(x + handW, y - handH);
        glVertex2f(x + handW, y);
        glVertex2f(x, y);
        glEnd();
    }

    private void drawPauseOverlay() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.5f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(width, 0);
        glVertex2f(width, height);
        glVertex2f(0, height);
        glEnd();
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    private IsoCube getSelectedCube() {
        float cx = width / 2f, cy = height / 2f;
        IsoCube best = null;
        double bestDist = Double.MAX_VALUE;
        for (IsoCube cube : cubes) {
            if (cube.isOverlapping(cx, cy)) {
                double dist = cube.distanceTo(cx, cy);
                if (dist < bestDist || (dist == bestDist && cube.x > best.x)) {
                    best = cube;
                    bestDist = dist;
                }
            }
        }
        return best;
    }

    private void removeCube(IsoCube cube) { cubes.remove(cube); }

    private void addCube(int x, int y, int z) {
        int size = Math.min(width, height) / 8;
        IsoCube newCube = new IsoCube(x, y, z, size, grassTexture);
        newCube.updateScreenPos();
        cubes.add(newCube);
    }

    static class IsoCube {
        int x, y, z, size;
        float screenX, screenY, halfW, halfH;
        float[] topFace, leftFace, rightFace;
        int texture;

        public IsoCube(int x, int y, int z, int size, int texture) {
            this.x = x; this.y = y; this.z = z; this.size = size; this.texture = texture;
            halfW = size/2f; halfH = size/4f;
        }

        public void updateScreenPos() {
            halfW = size / 2f;
            halfH = size / 4f;
            screenX = (x - z) * halfW + screenOffsetX;
            screenY = (x + z) * halfH - y * size / 2f + screenOffsetY;
            updateFaces();
        }

        private void updateFaces() {
            topFace = new float[]{screenX, screenY-halfH, screenX+halfW, screenY,
                                  screenX, screenY+halfH, screenX-halfW, screenY};
            leftFace = new float[]{screenX-halfW, screenY, screenX, screenY+halfH,
                                   screenX, screenY+halfH+size/2f, screenX-halfW, screenY+size/2f};
            rightFace = new float[]{screenX+halfW, screenY, screenX, screenY+halfH,
                                    screenX, screenY+halfH+size/2f, screenX+halfW, screenY+size/2f};
        }

        public void draw() {
            glBindTexture(GL_TEXTURE_2D, texture);
            drawQuad(topFace);
            drawQuad(leftFace);
            drawQuad(rightFace);
        }

        private void drawQuad(float[] face) {
            glBegin(GL_QUADS);
            glTexCoord2f(0,0); glVertex2f(face[0], face[1]);
            glTexCoord2f(1,0); glVertex2f(face[2], face[3]);
            glTexCoord2f(1,1); glVertex2f(face[4], face[5]);
            glTexCoord2f(0,1); glVertex2f(face[6], face[7]);
            glEnd();
        }

        public boolean isOverlapping(float px, float py) {
            float minX = screenX-halfW, maxX = screenX+halfW;
            float minY = screenY-halfH, maxY = screenY+halfH+size/2f;
            return px>=minX && px<=maxX && py>=minY && py<=maxY;
        }

        public double distanceTo(float px, float py) {
            return Math.hypot(screenX - px, screenY - py);
        }

        public void drawOutline() {
            glColor3f(1f, 1f, 0f);
            glLineWidth(2f);
            drawLineLoop(topFace);
            drawLineLoop(leftFace);
            drawLineLoop(rightFace);
            glLineWidth(1f);
        }

        private void drawLineLoop(float[] face) {
            glBegin(GL_LINE_LOOP);
            for (int i=0;i<face.length;i+=2) glVertex2f(face[i], face[i+1]);
            glEnd();
        }
    }

    private int loadTexture(String path) {
        int texID;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), comp = stack.mallocInt(1);
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = STBImage.stbi_load(path, w, h, comp, 4);
            if (image==null) throw new RuntimeException("Failed to load texture: "+path);
            texID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texID);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,w.get(),h.get(),0,GL_RGBA,GL_UNSIGNED_BYTE,image);
            STBImage.stbi_image_free(image);
        }
        return texID;
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
