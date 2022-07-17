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

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL20.*;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.dou.opengl.VideoBuffer.Type;
import org.dou.opengl.VideoBuffer.Usage;

/**
 * OpenGL shader program object wraper
 * 
 * @author Viktor Gubin
 */
public class Program {

  // program object OpenGL id
  private final int id;
  private final List<Shader> shaders;
  private final Deque<VideoBuffer> buffers;

  private Program(List<Shader> shaders) {
    this.id = glCreateProgram();
    this.shaders = shaders;
    for (Shader shader : shaders) {
      glAttachShader(id, shader.getId());
    }
    this.buffers = new LinkedList<VideoBuffer>();
  }

  /**
   * Returns build for loading shaders and constructing program object
   * 
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Links and validates this program object
   */
  public void link() {
    glLinkProgram(id);
    validateParam(GL_LINK_STATUS, "Shader program link error:\n");
    glValidateProgram(id);
    validateParam(GL_VALIDATE_STATUS, "Shader program validate error:\n");
    // release shader objects, since no longer needed and just taking resources
    for (Shader shader : shaders) {
      glDetachShader(id, shader.getId());
      shader.delete();
    }
  }

  private void validateParam(int pname, String errFormat) {
    int[] errc = new int[1];
    glGetProgramiv(id, pname, errc);
    if (GL_FALSE == errc[0]) {
      glGetProgramiv(id, GL_INFO_LOG_LENGTH, errc);
      throw new IllegalStateException(
          String.format("%s %s", errFormat, glGetProgramInfoLog(id, errc[1])));
    }
  }

  /**
   * Validates current OpenGL error state
   * 
   * @throws IllegalStateException in case of an OpenGL state error code
   * @throws OutOfMemoryError when no free video memory left
   */
  public static void validateOpenGL() {
    int errCode = glGetError();
    switch (errCode) {
      case GL_NO_ERROR:
        break;
      case GL_OUT_OF_MEMORY:
        throw new OutOfMemoryError("OpenGL can not allocate video memory");
      default:
        throw new IllegalStateException("OpenGL error: " + errCode);
    }
  }

  /**
   * Creates new OpenGL video buffer, i.e. allocates video memory and initialize it with the data
   * 
   * @param data - a data to be stored in video buffer
   * @param type - video buffer type
   * @param usage - vido buffer usage
   * @return new video buffer
   */
  public VideoBuffer createVideoBuffer(ByteBuffer data, Type type, Usage usage) {
    VideoBuffer result = new VideoBuffer(type, usage, GLType.BYTE);
    result.setData(data);
    buffers.push(result);
    return result;
  }

  /**
   * Creates new OpenGL video buffer, i.e. allocates video memory and initialize it with the data
   * 
   * @param data - a data to be stored in video buffer
   * @param type - video buffer type
   * @param usage - vido buffer usage
   * @return new video buffer
   */
  public VideoBuffer createVideoBuffer(ShortBuffer data, Type type, Usage usage) {
    VideoBuffer result = new VideoBuffer(type, usage, GLType.UNSIGNED_SHORT);
    result.setData(data);
    buffers.push(result);
    return result;
  }

  /**
   * Creates new OpenGL video buffer, i.e. allocates video memory and initialize it with the data
   * 
   * @param data - a data to be stored in video buffer
   * @param type - video buffer type
   * @param usage - vido buffer usage
   * @return new video buffer
   */
  public VideoBuffer createVideoBuffer(IntBuffer data, Type type, Usage usage) {
    VideoBuffer result = new VideoBuffer(type, usage, GLType.UNSIGNED_INT);
    result.setData(data);
    buffers.push(result);
    return result;
  }

  /**
   * Creates new OpenGL video buffer, i.e. allocates video memory and initialize it with the data
   * 
   * @param data - a data to be stored in video buffer
   * @param type - video buffer type
   * @param usage - vido buffer usage
   * @return new video buffer
   */
  public VideoBuffer createVideoBuffer(FloatBuffer data, Type type, Usage usage) {
    VideoBuffer result = new VideoBuffer(type, usage, GLType.FLOAT);
    result.setData(data);
    buffers.push(result);
    return result;
  }

  /**
   * Creates new OpenGL video buffer, i.e. allocates video memory and initialize it with the data
   * 
   * @param data - a data to be stored in video buffer
   * @param type - video buffer type
   * @param usage - vido buffer usage
   * @return new video buffer
   */
  public VideoBuffer createVideoBuffer(DoubleBuffer data, Type type, Usage usage) {
    VideoBuffer result = new VideoBuffer(type, usage, GLType.DOUBLE);
    result.setData(data);
    buffers.push(result);
    return result;
  }

  /**
   * Install this program as part of current rendering state
   */
  public void start() {
    glUseProgram(id);
  }

  /**
   * Sets current rendering state to default
   */
  public void stop() {
    glUseProgram(0);
  }

  /**
   * Locates a uniform id in this program
   * 
   * @param name - uniform name as specified in GLSL
   * @return uniform location or -1 if uniform is not found
   */
  public int getUniformLocation(String name) {
    return glGetUniformLocation(this.id, name);
  }

  /**
   * Passes vertex attribute array into program object
   * 
   * @param attrNo shader attribute number see {@link Program#bindAttribLocation}
   * @param vbo vertex buffer object to take data from
   * @param normalized whether attribute data needs to be normalized before passing to program
   * @param vertexSize size of vertex attribute components i.e. 2,3 or 4
   * @param stride - i.e. of whole vertex attribute size. For <code>{px,py,pz}{nz,ny,nz}</code>
   *        format would be 6 since 3 floats for position and 3 floats for normal vector
   * @param offset - offset from vertex attribute component begin, <code>{px,py,pz}{nz,ny,nz}</code>
   *        format should be 0 when binding position and 3 when binding normal vector
   */
  public void passVertexAttribArray(int attrNo, VideoBuffer vbo, boolean normalized, int vertexSize,
      int stride, int offset) {
    if (VideoBuffer.Type.ARRAY_BUFFER != vbo.getType()) {
      throw new IllegalArgumentException("Array buffer expected");
    }
    final int dtpSize = vbo.getDataType().sizeOf();
    int byteStride = stride * dtpSize;
    long componentOffset = offset * dtpSize;
    vbo.bind();
    glVertexAttribPointer(attrNo, vertexSize, vbo.getDataType().glEnum(), normalized, byteStride,
        componentOffset);
    validateOpenGL();
    glEnableVertexAttribArray(attrNo);
    validateOpenGL();
    vbo.unbind();
  }

  /**
   * Binds an shader attribute to a number
   * 
   * @param attrNo attribute number to bind
   * @param name attribute name as specified in vertex shader GLSL to bind
   */
  public void bindAttribLocation(int attrNo, String name) {
    glBindAttribLocation(this.id, attrNo, name);
    validateOpenGL();
  }

  /**
   * Passes whole vertex attributes into program with layout. Can be used only when all vertex
   * attributes are line stored in the single VBO
   * 
   * @param vbo - a vertex buffer object with the data
   * @param normalized whether attribute data needs to be normalized before passing to program
   * @param layout combination of vertex attribute name an vertex and size
   */
  public void passVertexAttribArray(VideoBuffer vbo, boolean normalized, Attribute... layout) {
    int vertexSize = 0;
    for (int i = 0; i < layout.length; i++) {
      bindAttribLocation(i, layout[i].getName());
      vertexSize += layout[i].getStride();
    }
    int offset = 0;
    for (int i = 0; i < layout.length; i++) {
      passVertexAttribArray(i, vbo, normalized, layout[i].getStride(), vertexSize, offset);
      offset += layout[i].getStride();
    }
  }

  /**
   * Deletes program object and free all associated resources. Should be called manually to prevent
   * video memory leaks
   */
  public void delete() {
    glDeleteProgram(this.id);
    while (!this.buffers.isEmpty()) {
      buffers.pop().delete();
    }
  }

  public static class Attribute {
    private final String name;
    private final int stride;

    public static Attribute of(String name, int stride) {
      return new Attribute(name, stride);
    }

    private Attribute(String name, int stide) {
      this.name = name;
      this.stride = stide;
    }

    public String getName() {
      return name;
    }

    public int getStride() {
      return stride;
    }
  }

  public static class Builder {

    private final List<Shader> shaders;

    Builder() {
      this.shaders = new ArrayList<>(6);
    }

    public Builder setVertex(Shader vertex) {
      shaders.add(vertex);
      return this;
    }

    public Builder setFragment(Shader fragment) {
      shaders.add(fragment);
      return this;
    }

    public Builder setCompute(Shader compute) {
      shaders.add(compute);
      return this;
    }

    public Builder setGeometry(Shader geometry) {
      shaders.add(geometry);
      return this;
    }

    public Builder setTessControl(Shader tessControl) {
      shaders.add(tessControl);
      return this;
    }

    public Builder setTessEvaluation(Shader tessEvaluation) {
      shaders.add(tessEvaluation);
      return this;
    }

    public Program build() {
      if (!shaders.stream().anyMatch(s -> Shader.Type.VERTEX == s.getType())) {
        throw new IllegalStateException("Vertex shader is manadatory");
      }
      if (!shaders.stream().anyMatch(s -> Shader.Type.FRAGMENT == s.getType())) {
        throw new IllegalStateException("Fragment shader is manadatory");
      }
      if (shaders.stream().anyMatch(s -> Shader.Type.TESS_CONTROL == s.getType())
          && !shaders.stream().anyMatch(s -> Shader.Type.TESS_EVALUATION == s.getType())) {
        throw new IllegalStateException(
            "Tesselation control shader exist, but no tesselation evaluation shader load");
      } else if (!shaders.stream().anyMatch(s -> Shader.Type.TESS_CONTROL == s.getType())
          && shaders.stream().anyMatch(s -> Shader.Type.TESS_EVALUATION == s.getType())) {
        throw new IllegalStateException(
            "Tesselation evaluetion shader exist, but no tesselation control shader load");
      }
      return new Program(shaders);
    }

  }

}
