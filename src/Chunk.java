import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class Chunk {

    static final int SIZE = 16;

    int vao, vbo;
    int vertexCount;

    int[][][] blocks = new int[SIZE][1][SIZE];

    public Chunk() {
        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                blocks[x][0][z] = 1;
    }

    void buildMesh() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(16_000);

        for (int x = 0; x < SIZE; x++)
            for (int z = 0; z < SIZE; z++)
                if (blocks[x][0][z] != 0)
                    addTopFace(buf, x, 0, z);

        buf.flip();
        vertexCount = buf.remaining() / 5;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3 * 4);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    void addTopFace(FloatBuffer b, float x, float y, float z) {
        b.put(x).put(y+1).put(z).put(0).put(0);
        b.put(x+1).put(y+1).put(z).put(1).put(0);
        b.put(x+1).put(y+1).put(z+1).put(1).put(1);

        b.put(x).put(y+1).put(z).put(0).put(0);
        b.put(x+1).put(y+1).put(z+1).put(1).put(1);
        b.put(x).put(y+1).put(z+1).put(0).put(1);
    }

    void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
