import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Camera {

    public float x = 8, y = 6, z = 20;
    float speed = 0.3f;

    public void update(long window) {
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) z -= speed;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) z += speed;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) x -= speed;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) x += speed;
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS) y -= speed;
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS) y += speed;
    }

    public void applyView() {
        glRotatef(35, 1, 0, 0);   // iso-like angle
        glRotatef(45, 0, 1, 0);
        glTranslatef(-x, -y, -z);
    }
}
