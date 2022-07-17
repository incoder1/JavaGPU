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

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.lwjgl.opengl.ARBQueryBufferObject;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

/**
 * 
 * OpenGL video buffer object helper
 * 
 * @author Viktor Gubin
 *
 */
public final class VideoBuffer {

  private final int id;
  private int size;
  private final Type type;
  private final Usage usage;
  private final GLType dataType;

  VideoBuffer(Type type, Usage usage, GLType dataType) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      IntBuffer px = stack.mallocInt(1);
      glGenBuffers(px);
      this.id = px.get(0);
    }
    this.size = 0;
    this.type = type;
    this.usage = usage;
    this.dataType = dataType;
  }


  void setData(ByteBuffer data) {
    this.size = data.remaining();
    glBindBuffer(type.glEnum(), this.id);
    try {
      glBufferData(type.glEnum(), data, usage.glEnum());
    } finally {
      glBindBuffer(type.glEnum(), 0);
    }
  }

  void setData(ShortBuffer data) {
    this.size = data.remaining();
    glBindBuffer(type.glEnum(), this.id);
    try {
      glBufferData(type.glEnum(), data, usage.glEnum());
    } finally {
      glBindBuffer(type.glEnum(), 0);
    }
  }

  void setData(IntBuffer data) {
    this.size = data.remaining();
    glBindBuffer(type.glEnum(), this.id);
    try {
      glBufferData(type.glEnum(), data, usage.glEnum());
    } finally {
      glBindBuffer(type.glEnum(), 0);
    }
  }

  void setData(FloatBuffer data) {
    this.size = data.remaining();
    glBindBuffer(type.glEnum(), this.id);
    try {
      glBufferData(type.glEnum(), data, usage.glEnum());
    } finally {
      glBindBuffer(type.glEnum(), 0);
    }
  }

  void setData(DoubleBuffer data) {
    this.size = data.remaining();
    glBindBuffer(type.glEnum(), this.id);
    try {
      glBufferData(type.glEnum(), data, usage.glEnum());
    } finally {
      glBindBuffer(type.glEnum(), 0);
    }
  }


  /**
   * Returns this buffer OpenGL native identifier
   * @return this buffer native identifier
   */  
  public int getId() {
    return id;
  }

  /**
   * Returns this buffer size in elements
   * @return this buffer size in elements
   */  
  public int getSize() {
    return size;
  }

  /**
   * Returns this buffer OpenGL type enumeration
   * @return this buffer type enumeration
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns this buffer OpenGL usage enumeration
   * @return this buffer usage enumeration
   */
  public Usage getUsage() {
    return usage;
  }

  /**
   * Returns this buffer OpenGL binding data type enumeration
   * @return this buffer binding data type enumeration
   */
  public GLType getDataType() {
    return dataType;
  }

  /**
   * Binds this buffer to OpengGL current state
   */
  public void bind() {
    glBindBuffer(type.glEnum(), this.id);
  }

  /**
   * Binds O index to OpengGL current buffer state
   */
  public void unbind() {
    glBindBuffer(type.glEnum(), 0);
  }

  void delete() {
    glDeleteBuffers(new int[] {this.id});
  }

  /**
   * OpenGL 4.3+ supported buffer type
   */
  public static enum Type {
    /**
     * Vertex attributes. Requires: OpenGL 1.5+
     */
    ARRAY_BUFFER(GL_ARRAY_BUFFER),
    /**
     * Atomic counter storage. Requires: OpenGL 4.2+
     */
    ATOMIC_COUNTER(GL42.GL_ATOMIC_COUNTER_BUFFER),
    /**
     * Buffer copy source. Requires: OpenGL 3.1+
     */
    COPY_READ_BUFFER(GL31.GL_COPY_READ_BUFFER),
    /**
     * Buffer copy destination. Requires: OpenGL 3.1+
     */
    COPY_WRITE_BUFFER(GL31.GL_COPY_WRITE_BUFFER),
    /**
     * Indirect compute dispatch commands. Requires: OpenGL 4.0+
     */
    DISPATCH_INDIRECT_BUFFER(GL43.GL_DISPATCH_INDIRECT_BUFFER),
    /**
     * Indirect command arguments. Requires: OpenGL 4.0+
     */
    DRAW_INDIRECT_BUFFER(GL40.GL_DRAW_INDIRECT_BUFFER),
    /**
     * Vertex array indexes. Requires: OpenGL 1.5+
     */
    ELEMENT_ARRAY_BUFFER(GL_ELEMENT_ARRAY_BUFFER),
    /**
     * Query result buffer. Requires: OpenGL 3.1+
     */
    QUERY_BUFFER(ARBQueryBufferObject.GL_QUERY_BUFFER),
    /**
     * Pixel read target. Requires: OpenGL 2.1+
     */
    PIXEL_PACK_BUFFER(GL21.GL_PIXEL_PACK_BUFFER),
    /**
     * Texture data source. Requires: OpenGL 2.1+
     */
    PIXEL_UNPACK_BUFFER(GL21.GL_PIXEL_UNPACK_BUFFER),
    /**
     * Read-write storage for shaders. Requires: OpenGL 4.3+
     */
    SHADER_STORAGE_BUFFER(GL43.GL_SHADER_STORAGE_BUFFER),
    /**
     * Texture data buffer.Requires: OpenGL 3.1+
     */
    TEXTURE_BUFFER(GL31.GL_TEXTURE_BUFFER),
    /**
     * Transform feedback buffer. Requires: OpenGL 4.0+
     */
    TRANSFORM_FEEDBACK_BUFFER(35982),
    /**
     * Uniform block storage. Requires: OpenGL 3.1+
     */
    UNIFORM_BUFFER(GL31.GL_UNIFORM_BUFFER);

    private final int gl;

    private Type(int gl) {
      this.gl = gl;
    }

    /**
     * Returns OpenGL enumeration value to pass into OpenGL functions
     * 
     * @return OpenGL enumeration value
     */
    public int glEnum() {
      return gl;
    }

  }

  /**
   * OpenGL buffer usage, specifies the expected usage pattern of the data store. The frequency of
   * access may be one of these STREAM
   * 
   * The data store contents will be modified once and used at most a few times. STATIC
   * 
   * The data store contents will be modified once and used many times. DYNAMIC
   * 
   * The data store contents will be modified repeatedly and used many times.
   * 
   * The nature of access may be one of these:
   * 
   * DRAW
   * 
   * The data store contents are modified by the application, and used as the source for GL drawing
   * and image specification commands. READ
   * 
   * The data store contents are modified by reading data from the GL, and used to return that data
   * when queried by the application. COPY
   * 
   * The data store contents are modified by reading data from the GL, and used as the source for GL
   * drawing and image specification commands.
   */
  public enum Usage {

    /**
     * Dynamic and copy combination, see enum description
     */
    DYNAMIC_COPY(GL_DYNAMIC_COPY),
    /**
     * Dynamic and draw combination, see enum description
     */
    DYNAMIC_DRAW(GL_DYNAMIC_DRAW),
    /**
     * Dynamic and read combination, see enum description
     */
    DYNAMIC_READ(GL_DYNAMIC_READ),
    /**
     * Static and copy combination, see enum description
     */
    STATIC_COPY(GL_STATIC_COPY),
    /**
     * Static and draw combination, see enum description
     */
    STATIC_DRAW(GL_STATIC_DRAW),
    /**
     * Static and copy combination, see enum description
     */
    STATIC_READ(GL_STATIC_READ),
    /**
     * Stream and copy combination, see enum description
     */
    STREAM_COPY(GL_STREAM_COPY),
    /**
     * Stream and draw combination, see enum description
     */
    STREAM_DRAW(GL_STREAM_DRAW),
    /**
     * Stream and read combination, see enum description
     */
    STREAM_READ(GL_STREAM_READ);

    private final int gl;

    private Usage(final int gl) {
      this.gl = gl;
    }

    /**
     * Returns OpenGL enumeration value to pass into OpenGL functions
     * 
     * @return OpenGL enumeration value
     */
    public int glEnum() {
      return this.gl;
    }

  }

}
