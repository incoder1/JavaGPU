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
package org.dou.opencl;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;
import org.lwjgl.system.MemoryStack;

public class MultArrays {

  private static final String KERNEL =
      "kernel void mul_arrays(global const float *a, global const float *b, global float *answer) { \n"
      + " unsigned int xid = get_global_id(0); \n"
      + " answer[xid] = a[xid] * b[xid]; \n"
      + " } \n";

  private static final float[] LEFT_ARRAY = {1F, 3F, 5F, 7F};
  private static final float[] RIGHT_ARRAY = {2F, 4F, 6F, 8F};


  public static void main(String[] args) {
    try (ClRuntime cl = new ClRuntime(); MemoryStack stack = MemoryStack.stackPush();) {
      ClRuntime.Platform platform = cl.getPlatforms().first();
      ClRuntime.Device device = platform.getDefault();
      try (ClRuntime.Context context = device.createContext();
          ClRuntime.Program program = context.createProgramWithSource(KERNEL)) {

        FloatBuffer lhs = stack.floats(LEFT_ARRAY);
        FloatBuffer rhs = stack.floats(RIGHT_ARRAY);

        printSequence("Left hand statement: ", lhs, System.out);
        printSequence("Right hand statement: ", rhs, System.out);

        int gws = LEFT_ARRAY.length * Float.BYTES;

        ClRuntime.CommandQueue cq = program.getCommandQueue();

        final ClRuntime.VideoMemBuffer first = cq.hostPtrReadBuffer(lhs);
        final ClRuntime.VideoMemBuffer second = cq.hostPtrReadBuffer(rhs);
        final ClRuntime.VideoMemBuffer answer = cq.createReadWriteBuffer((int) gws);

        cq.flush();

        ClRuntime.Kernel sumVectors = program.createKernel("mul_arrays");
        sumVectors.arg(first).arg(second).arg(answer).executeAsDataParallel(gws);

        ByteBuffer result = stack.calloc(answer.getCapacity());
        cq.readVideoMemory(answer, result);

        printSequence("Result: ", result.asFloatBuffer(), System.out);

      } catch (ExecutionException exc) {
        System.err.println(exc.getMessage());
        System.exit(-1);
      }
    }
  }

  private static void printSequence(String label, FloatBuffer sequence, PrintStream to) {
    to.print(label);
    to.print(": [ ");
    int i = 0;
    to.print(Float.toString(sequence.get(i)));
    ++i;
    while (i < sequence.limit()) {
      to.print(", ");
      to.print(Float.toString(sequence.get(i)));
      ++i;
    }
    to.println(" ]");
  }

}
