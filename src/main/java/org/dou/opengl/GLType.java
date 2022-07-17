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

import org.lwjgl.opengl.GL11;

/**
 * Pre-defined OpenGL types that should be available to various bindings enumeration
 */
public enum GLType {
  /**
   * Bitdepth 8, Signed, 2's complement binary integer
   */
  BYTE(GL11.GL_BYTE, Byte.BYTES),
  /**
   * Bitdepth 8, Unsigned binary integer
   */
  UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE, Byte.BYTES),
  /**
   * Bitdepth 16, Signed, 2's complement binary integer
   */
  SHORT(GL11.GL_SHORT, Short.BYTES),
  /**
   * Bitdepth 16, Unsigned binary integer
   */
  UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT, Short.BYTES),
  /**
   * Bitdepth 32, Signed, 2's complement binary integer
   */
  INT(GL11.GL_INT, Integer.BYTES),
  /**
   * Bitdepth 32, Unsigned binary integer
   */
  UNSIGNED_INT(GL11.GL_UNSIGNED_INT, Integer.BYTES),
  /**
   * Bitdepth 32, An IEEE-754 floating-point value
   */
  FLOAT(GL11.GL_FLOAT, Float.BYTES),

  /**
   * Bitdepth 64, An IEEE-754 floating-point value
   */
  DOUBLE(GL11.GL_DOUBLE, Double.BYTES);

  private final int gl;
  private final int elementSize;

  private GLType(final int gl, final int size) {
    this.gl = gl;
    this.elementSize = size;
  }

  /**
   * Returns size of the type in bytes (8 bit per byte)
   * 
   * @return size of the type in bytes
   */
  public int sizeOf() {
    return this.elementSize;
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
