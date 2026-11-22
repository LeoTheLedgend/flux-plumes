package dev.leo.fluxplumes.client;

import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL41C.glProgramUniform1f;
import static org.lwjgl.opengl.GL41C.glProgramUniform3f;
import static org.lwjgl.opengl.GL41C.glProgramUniform3fv;

/** Small helpers for setting uniforms on a shader program; silently skips uniforms the shader doesn't have. */
public final class GlUniforms {
    private GlUniforms() {}

    public static void set1f(int program, String name, float v) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) {
            glProgramUniform1f(program, loc, v);
        }
    }

    public static void set3f(int program, String name, float x, float y, float z) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) {
            glProgramUniform3f(program, loc, x, y, z);
        }
    }

    public static void set3fv(int program, String name, float[] values) {
        int loc = glGetUniformLocation(program, name);
        if (loc != -1) {
            glProgramUniform3fv(program, loc, values);
        }
    }
}
