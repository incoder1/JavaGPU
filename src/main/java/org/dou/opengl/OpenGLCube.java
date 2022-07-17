/*
 * Copyright 2020 Viktor Gubin
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.dou.opengl;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.nglDrawElements;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.dou.opengl.Program.Attribute;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * This example renders phong shading cube using OpenGL 4.2 +
 * 
 * @author Viktor Gubin
 */
public class OpenGLCube {

  // Vertex shader resource
  private static final URL VERTEX = OpenGLCube.class.getResource("cube.vtx.glsl");
  // Fragment shader resource
  private static final URL FRAGEMENT = OpenGLCube.class.getResource("cube.frag.glsl");

  public static void main(String[] args) {
    // initialize GLFW
    GLFWErrorCallback.createPrint(System.err).set();
    if (!glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }
    // Create a window and OpenGL context for it
    try (Window window = new Window(800, 600, "Cube")) {
      // Create OpenGL program object
      final Program program =
          Program.builder().setVertex(Shader.load(VERTEX.openStream(), Shader.Type.VERTEX))
              .setFragment(Shader.load(FRAGEMENT.openStream(), Shader.Type.FRAGMENT)).build();
      // crate render operation callback
      final Scene scene = new Scene(program);
      window.screenCenterify();
      // show window and render scene
      window.show(scene);
    } catch (IOException exc) {
      throw new IllegalStateException("Can not read shader from resource", exc);
    } finally {
      // dispose native resources
      glfwTerminate();
      glfwSetErrorCallback(null).free();
    }
  }

  private static class Scene implements Renderable, AutoCloseable {

     // Linking Vertex Attributes
    
    
    // VBO
    // Position and normal is interleaving according to the performance best practice https://www.khronos.org/opengl/wiki/Vertex_Specification_Best_Practices
    private static final float[] VERTEX = {
        // position            |     normal
        // left
         1.0F,  1.0F,  1.0F, /*|*/  1.0F, 0.0F, 0.0F, 
         1.0F,  1.0F, -1.0F, /*|*/  1.0F, 0.0F, 0.0F,
         1.0F, -1.0F, -1.0F, /*|*/  1.0F, 0.0F, 0.0F, 
         1.0F, -1.0F,  1.0F, /*|*/  1.0F, 0.0F, 0.0F,
        // front
        -1.0F,  1.0F,  1.0F, /*|*/  0.0F, 0.0F, 1.0F,
         1.0F,  1.0F,  1.0F, /*|*/  0.0F, 0.0F, 1.0F, 
         1.0F, -1.0F,  1.0F, /*|*/  0.0F, 0.0F, 1.0F, 
        -1.0F, -1.0F,  1.0F, /*|*/  0.0F, 0.0F, 1.0F,
        // top
        -1.0F, 1.0F,  1.0F,  /*|*/  0.0F, 1.0F, 0.0F,
        -1.0F, 1.0F, -1.0F,  /*|*/  0.0F, 1.0F, 0.0F,
         1.0F, 1.0F, -1.0F,  /*|*/  0.0F, 1.0F, 0.0F,
         1.0F, 1.0F,  1.0F,  /*|*/  0.0F, 1.0F, 0.0F,
        // bottom
        -1.0F, -1.0F,  1.0F,  /*|*/  0.0F, -1.0F, 0.0F,
        -1.0F, -1.0F, -1.0F, /*|*/   0.0F, -1.0F, 0.0F, 
         1.0F, -1.0F, -1.0F,  /*|*/  0.0F, -1.0F,  0.0F,
         1.0F, -1.0F,  1.0F,  /*|*/  0.0F, -1.0F, 0.0F,
        // right
        -1.0F,  1.0F,  1.0F,  /*|*/ -1.0F, 0.0F, 0.0F,
        -1.0F,  1.0F, -1.0F,  /*|*/ -1.0F, 0.0F, 0.0F,
        -1.0F, -1.0F, -1.0F,  /*|*/ -1.0F, 0.0F, 0.0F,
        -1.0F, -1.0F,  1.0F,  /*|*/ -1.0F, 0.0F, 0.0F,
        // back
        -1.0F,  1.0F, -1.0F,  /*|*/  0.0F, 0.0F, -1.0F,
         1.0F,  1.0F, -1.0F,  /*|*/  0.0F, 0.0F, -1.0F,
         1.0F, -1.0F, -1.0F,  /*|*/  0.0F, 0.0F, -1.0F, 
        -1.0F, -1.0F, -1.0F,  /*|*/  0.0F, 0.0F, -1.0F
    };

    // IBO, see details https://learnopengl.com/Getting-started/Hello-Triangle
    private static final short[] INDECIES = {
        0,  1,  3,  1,  2,  3,  // left quad
        4,  5,  7,  5,  6,  7,  // front quad
        8,  9,  11, 9, 10,  11, // top quad
        12, 13, 15, 13, 14, 15, // bottom quad
        16, 17, 19, 17, 18, 19, // right quad
        20, 21, 23, 21, 22, 23  // back quad
    };

    // Light color. See https://learnopengl.com/Lighting/Basic-Lighting
    private static float[] LIGHT = {-0.5F, 0.5F, -5.5F, 1.0F, // position
        0.0F, 0.0F, 0.0F, 1.0F, // ambient color
        1.0F, 1.0F, 1.0F, 1.0F, // diffuse color
        1.0F, 1.0F, 1.0F, 0.0F // specular color
    };

    // Material, a white plasic. See  https://learnopengl.com/Lighting/Materials
    private static float[] MATERIAL = {
        0.0F, 0.0F, 0.0F, 1.0F, // ambient
        0.4F, 0.4F, 0.4F, 1.0F, // diffuse
        0.7F, 0.0F, 0.0F, 1.0F, // specular
        0.0F, 0.0F, 0.0F, 1.0F // emission
    };

    // material shininess
    private static final float SHININESS = 32.0F;


    // OpenGL program object
    private final Program program;

    // vertex array object id
    private int vao;

    // model-view matrix uniform location
    private final int mvUL;
    // normal matrix uniform location
    private int nmUL;
    // model-view-projection matrix uniform location
    private int mvpUL;

    // light pads matrix uniform location
    private int lightUL;
    // material adse matrix uniform location
    private int materialUL;
    private int materialShininessUL;

    public Scene(final Program program) {

      this.program = program;

      try (MemoryStack stack = MemoryStack.stackPush()) {
        // Create vertex array buffer
        IntBuffer px = stack.mallocInt(1);
        glGenVertexArrays(px);
        this.vao = px.get();

        // Create vertex buffer object
        VideoBuffer vbo = program.createVideoBuffer(stack.floats(VERTEX),
            VideoBuffer.Type.ARRAY_BUFFER, VideoBuffer.Usage.STATIC_DRAW);

        // Create vertex index buffer object
        VideoBuffer vio = program.createVideoBuffer(stack.shorts(INDECIES),
            VideoBuffer.Type.ELEMENT_ARRAY_BUFFER, VideoBuffer.Usage.STATIC_DRAW);

        // now bind them both to vao
        glBindVertexArray(vao);
        vio.bind();
        // bind VBO to shader attributes
        program.passVertexAttribArray(vbo, false, Attribute.of("vertex_coord", 3),
            Attribute.of("vertex_normal", 3));
        // unbind vao, state will leave as it is, vio should not be unbound
        glBindVertexArray(0);
      }

      // now we need to link a program, after that we can not build VAO
      program.link();

      // now we can locate uniforms from program
      this.mvpUL = program.getUniformLocation("mvp");
      this.mvUL = program.getUniformLocation("mv");
      this.nmUL = program.getUniformLocation("nm");

      this.lightUL = program.getUniformLocation("light_pads");
      this.materialUL = program.getUniformLocation("material_adse");
      this.materialShininessUL = program.getUniformLocation("material_shininess");
    }

    @Override
    public void close() throws Exception {
      program.delete();
    }

    @Override
    public void render(int width, int height) {

      // calculate perspective for our context
      // those works similar to gluPerspective
      float fovY = (float) height / (float) width;
      float aspectRatio = (float) width / (float) height;
      float h = fovY * 2.0F;
      float w = h * aspectRatio;
      final Matrix4f projection = new Matrix4f().frustum(-w, w, -h, h, 2.0F, 10.0F);

      final Matrix4f modelView = new Matrix4f().identity();
      // move model far from eye position
      modelView.translate(0, 0, -5f);

      // rotate model a little by x an y axis, to see cube in projection
      modelView.rotateXYZ((float) Math.toRadians(20.0), -(float) Math.toRadians(45.0f), 0.0F);

      // calculate normal matrix to be used in Phong shading
      final Matrix4f normal = new Matrix4f();
      modelView.normal(normal);

      // take MVP
      final Matrix4f modelVeiwProjection = new Matrix4f().identity().mul(projection).mul(modelView);

      try (MemoryStack stack = MemoryStack.stackPush()) {

        // take matrix data native pointers
        FloatBuffer mv = stack.callocFloat(16);
        modelView.get(mv);
        FloatBuffer nm = stack.callocFloat(16);
        normal.get(nm);
        FloatBuffer mvp = stack.callocFloat(16);
        modelVeiwProjection.get(mvp);

        // render
        program.start();

        glUniformMatrix4fv(mvpUL, false, mvp);
        glUniformMatrix4fv(mvUL, false, mv);
        glUniformMatrix4fv(nmUL, false, nm);

        glUniformMatrix4fv(lightUL, false, LIGHT);
        glUniformMatrix4fv(materialUL, false, MATERIAL);
        glUniform1f(materialShininessUL, SHININESS);

        glBindVertexArray(vao);
        nglDrawElements(GL11.GL_TRIANGLES, INDECIES.length, GLType.UNSIGNED_SHORT.glEnum(),
            MemoryUtil.NULL);
        glBindVertexArray(0);

        program.stop();
      }

    }

  }

}
