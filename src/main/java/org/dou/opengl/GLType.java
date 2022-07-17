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

public enum GLType {
  BYTE(GL11.GL_BYTE, Byte.BYTES), UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE, Byte.BYTES), SHORT(
      GL11.GL_SHORT, Short.BYTES), UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT, Short.BYTES), INT(
          GL11.GL_INT, Integer.BYTES), UNSIGNED_INT(GL11.GL_UNSIGNED_INT, Integer.BYTES), FLOAT(
              GL11.GL_FLOAT, Float.BYTES), DOUBLE(GL11.GL_DOUBLE, Double.BYTES);

  private final int gl;
  private final int elementSize;

  private GLType(final int gl, final int size) {
    this.gl = gl;
    this.elementSize = size;
  }

  public int sizeOf() {
    return this.elementSize;
  }

  public int glEnum() {
    return this.gl;
  }
}
