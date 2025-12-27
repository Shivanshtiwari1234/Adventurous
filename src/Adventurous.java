import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.opengl.GL;

public class Adventurous {

    long window;
    Camera camera;
    Chunk chunk;
    Renderer renderer;

    boolean running = true;

    public static void main(String[] args) {
        new Adventurous().run();
    }

    void run() {
        init();
        loop();
        cleanup();
    }

    void init() {
        glfwInit();

        window = glfwCreateWindow(1280, 720, "Adventurous", NULL, NULL);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        renderer = new Renderer();
        renderer.init();

        camera = new Camera();
        chunk = new Chunk();
        chunk.buildMesh();
    }

    void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            camera.update(window);

            renderer.beginFrame(camera);
            chunk.render();
            renderer.endFrame();

            glfwSwapBuffers(window);
        }
    }

    void cleanup() {
        chunk.cleanup();
        glfwTerminate();
    }
}
