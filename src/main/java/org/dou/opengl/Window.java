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

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Window implements AutoCloseable {

  private final long handle;

  private static final int CLEAR_FLAGS =
      GL_ACCUM_BUFFER_BIT | GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT;

  public Window(int w, int h, String title) {

    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_DOUBLEBUFFER, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_RELEASE_BEHAVIOR, GLFW_RELEASE_BEHAVIOR_FLUSH);

    // the window will stay hidden after creation
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    // the window will be resizable
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);

    this.handle = glfwCreateWindow(w, h, title, MemoryUtil.NULL, MemoryUtil.NULL);
    if (this.handle == MemoryUtil.NULL) {
      throw new IllegalStateException("Failed to create the GLFW window");
    }
    // Close window and exit program on user press ESC
    glfwSetKeyCallback(handle, new GLFWKeyCallback() {
      @Override
      public void invoke(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
          glfwSetWindowShouldClose(window, true);
        }
      }
    });
    glfwMakeContextCurrent(this.handle);
    glfwSwapInterval(1);
    // load OpenGL native
    GLCapabilities glCapabilities = GL.createCapabilities(false);
    if (null == glCapabilities) {
      throw new IllegalStateException("Failed to load OpenGL native");
    }
    // Enable depth testing for z-culling
    glEnable(GL_DEPTH_TEST);
    // Set the type of depth-test
    glDepthFunc(GL_LEQUAL);
    // Enable smooth shading
    glShadeModel(GL_SMOOTH);

    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);


  }

  public void show(Renderable render) {

    glfwShowWindow(this.handle);

    while (!glfwWindowShouldClose(this.handle)) {
      glClear(CLEAR_FLAGS);
      glClearDepth(1.0F);

      int w[] = {0};
      int h[] = {0};
      glfwGetFramebufferSize(this.handle, w, h);
      glViewport(0, 0, w[0], h[0]);

      glClearColor(1.0F, 1.0F, 1.0F, 1.0F);

      if (null != render) {
        render.render(w[0], h[0]);
      }

      glfwSwapBuffers(this.handle);
      glfwWaitEvents();
    }

  }

  public void screenCenterify() {
    // Get the thread stack and push a new frame
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer pWidth = stack.mallocInt(1);
      IntBuffer pHeight = stack.mallocInt(1);
      // Get the window size passed to glfwCreateWindow
      glfwGetFramebufferSize(this.handle, pWidth, pHeight);
      // Get the resolution of the primary monitor
      GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
      // Center the window
      glfwSetWindowPos(this.handle, (vidmode.width() - pWidth.get(0)) / 2,
          (vidmode.height() - pHeight.get(0)) / 2);
    } // the stack frame is popped automatically
  }

  @Override
  public void close() throws IllegalStateException {
    glfwDestroyWindow(this.handle);
  }
  
}
