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

import static org.lwjgl.opencl.CL.create;
import static org.lwjgl.opencl.CL.createPlatformCapabilities;
import static org.lwjgl.opencl.CL.destroy;
import static org.lwjgl.opencl.CL.getICD;
import static org.lwjgl.opencl.CL10.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CLCapabilities;
import org.lwjgl.opencl.KHRICD;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * OpenCL use-load helper
 * 
 * @author Viktor Gubin
 */
public final class ClRuntime implements AutoCloseable {

  public ClRuntime() {
    CLCapabilities capatibilities = getICD();
    if (null == capatibilities || 0L == capatibilities.clCreateContext) {
      create();
    }
  }

  @Override
  public void close() throws RuntimeException {
    destroy();
  }

  private static void validateCL(int errCode, String errMessage) {
    if (errCode != CL_SUCCESS) {
      throw new RuntimeException(String.format("OpenCL error [%d]. %s", errCode, errMessage));
    }
  }

  private static void validateCL(int errCode) {
    if (errCode != CL_SUCCESS) {
      throw new RuntimeException(String.format("OpenCL error [%d].", errCode));
    }
  }

  private static String getPlatformInfoStringASCII(long cl_platform_id, int paramName) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      PointerBuffer pp = stack.mallocPointer(1);
      validateCL(clGetPlatformInfo(cl_platform_id, paramName, (ByteBuffer) null, pp));
      int bytes = (int) pp.get(0);

      ByteBuffer buffer = stack.malloc(bytes);
      validateCL(clGetPlatformInfo(cl_platform_id, paramName, buffer, null));

      return MemoryUtil.memASCII(buffer, bytes - 1);
    }
  }

  private static String getPlatformInfoStringUTF8(long cl_platform_id, int paramName) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      PointerBuffer pp = stack.mallocPointer(1);
      validateCL(clGetPlatformInfo(cl_platform_id, paramName, (ByteBuffer) null, pp));
      int bytes = (int) pp.get(0);

      ByteBuffer buffer = stack.malloc(bytes);
      validateCL(clGetPlatformInfo(cl_platform_id, paramName, buffer, null));

      return MemoryUtil.memUTF8(buffer, bytes - 1);
    }
  }

  /**
   * Returns list of OpenCL platforms supported by this environment
   * 
   * @return list of OpenCL platforms
   */
  public NavigableSet<Platform> getPlatforms() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      NavigableSet<Platform> result = new TreeSet<>();
      IntBuffer pi = stack.mallocInt(1);
      validateCL(clGetPlatformIDs(null, pi));
      if (pi.get(0) == 0) {
        throw new IllegalStateException("No OpenCL platforms found.");
      }
      PointerBuffer ids = stack.mallocPointer(pi.get(0));
      validateCL(clGetPlatformIDs(ids, (IntBuffer) null));
      for (int i = 0; i < ids.capacity(); i++) {
        CLCapabilities caps = createPlatformCapabilities(ids.get());
        if (caps.cl_khr_gl_sharing || caps.cl_APPLE_gl_sharing) {
          result.add(new Platform(ids.get(i), caps));
        }

      }
      return result;
    }
  }

  /**
   * OpenCL platform helper
   */
  public static final class Platform implements Comparable<Platform> {

    private final long id;
    private final CLCapabilities capabilities;

    private Platform(long id, CLCapabilities capabilities) {
      this.id = id;
      this.capabilities = capabilities;
    }

    /**
     * Returns this platform vendor name string
     * 
     * @return platform vendor name
     */
    public String getVendor() {
      return capabilities.cl_khr_icd
          ? getPlatformInfoStringASCII(id, KHRICD.CL_PLATFORM_ICD_SUFFIX_KHR)
          : getPlatformInfoStringUTF8(id, CL_PLATFORM_VENDOR);
    }

    private NavigableSet<Device> getDevices(int deviceType) {
      NavigableSet<Device> result = Collections.emptyNavigableSet();
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer pi = stack.mallocInt(1);
        int errcode = clGetDeviceIDs(id, deviceType, null, pi);
        if (errcode != CL_DEVICE_NOT_FOUND) {
          validateCL(errcode);
          PointerBuffer deviceIDs = stack.mallocPointer(pi.get(0));
          validateCL(clGetDeviceIDs(this.id, deviceType, deviceIDs, (IntBuffer) null));
          result = new TreeSet<>();
          for (int i = 0; i < deviceIDs.capacity(); i++) {
            result.add(new Device(deviceIDs.get(i), deviceType));
          }
        }
      }
      return result;
    }

    /**
     * Returns list of GPU based devices provided by this platform
     * 
     * @return list of GPU based devices
     */
    public NavigableSet<Device> getGPUDevices() {
      return getDevices(CL_DEVICE_TYPE_GPU);
    }

    /**
     * Returns list of CPU based devices provided by this platform
     * 
     * @return list of �PU based devices
     */
    public NavigableSet<Device> getCPUDevices() {
      return getDevices(CL_DEVICE_TYPE_GPU);
    }

    /**
     * Returns list of accelerator based devices provided by this platform
     * 
     * @return list of accelerator based devices
     */
    public NavigableSet<Device> getAcceleratorDevices() {
      return getDevices(CL_DEVICE_TYPE_ACCELERATOR);
    }

    /**
     * Returns default device for this platform
     * 
     * @return default device
     */
    public Device getDefault() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        final PointerBuffer deviceIDs = stack.mallocPointer(1);
        validateCL(clGetDeviceIDs(this.id, CL_DEVICE_TYPE_DEFAULT, deviceIDs, (IntBuffer) null),
            "Can not obtain OpenCL default device");
        final ByteBuffer typeBuf = stack.malloc(Long.BYTES);
        validateCL(clGetDeviceInfo(deviceIDs.get(0), CL_DEVICE_TYPE, typeBuf, null));
        return new Device(deviceIDs.get(0), typeBuf.getInt());
      }
    }

    @Override
    public int compareTo(Platform o) {
      return Long.compare(this.id, o.id);
    }

  }

  /**
   * OpenCL device helper
   */
  public static final class Device implements Comparable<Device> {

    private final long id;

    private final int type;

    private Device(long id, int type) {
      this.id = id;
      this.type = type;
    }

    private String getInfoStringUTF8(int paramName) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer pp = stack.mallocPointer(1);
        validateCL(clGetDeviceInfo(this.id, paramName, (ByteBuffer) null, pp));
        int bytes = (int) pp.get(0);
        ByteBuffer buffer = stack.malloc(bytes);
        validateCL(clGetDeviceInfo(this.id, paramName, buffer, null));
        return MemoryUtil.memUTF8(buffer, bytes - 1);
      }
    }

    public long getId() {
      return id;
    }

    public int getType() {
      return type;
    }

    public boolean isAccellerator() {
      return CL_DEVICE_TYPE_ACCELERATOR == this.type;
    }

    public boolean isCPU() {
      return CL_DEVICE_TYPE == this.type;
    }

    public boolean isGPU() {
      return CL_DEVICE_TYPE_GPU == this.type;
    }

    public String getName() {
      return getInfoStringUTF8(CL_DEVICE_NAME);
    }

    /**
     * Creates OpenCL context
     * 
     * @return new OpenCL context
     */
    public Context createContext() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = stack.mallocInt(1);
        PointerBuffer ptr = stack.mallocPointer(1).put(MemoryUtil.NULL).flip();
        long contextId = clCreateContext(ptr, this.id, null, 0, err);
        validateCL(err.get(0), "Can not create OpenCL context");
        return new Context(this, contextId);
      }
    }

    @Override
    public int compareTo(Device o) {
      return Long.compare(this.id, o.id);
    }

  }

  /**
   * OpenCL context helper
   */
  public static final class Context implements AutoCloseable {
    private final Device device;
    private final long id;

    private Context(Device device, long id) {
      this.device = device;
      this.id = id;
    }

    public long getId() {
      return id;
    }

    /**
     * Creates OpenCL command queue object
     * 
     * @return command queue object
     */
    private CommandQueue createCommandQueue() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = (IntBuffer) stack.mallocInt(1).put(CL_SUCCESS).flip();
        long cqId = clCreateCommandQueue(id, device.getId(), 0, err);
        validateCL(err.get(0), "Can not create command queue");
        return new CommandQueue(this, cqId);
      }
    }

    /**
     * Creates OpenCL program and load shader to it
     * 
     * @param source - OpenCL C shader source
     * @return new OpenCL program
     */
    public Program createProgramWithSource(String source) {
      long programId = 0;
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = stack.mallocInt(1);
        programId = clCreateProgramWithSource(this.id, source, err);
        validateCL(err.get(0), "Can not create OpenCL programm");
      }
      int errCode = clBuildProgram(programId, this.device.getId(), "", null, 0);
      switch (errCode) {
        case CL_SUCCESS:
          break;
        case CL_BUILD_PROGRAM_FAILURE:
          throw new IllegalStateException("Failure to build the program executable"
              + getProgramBuildInfo(CL_PROGRAM_BUILD_LOG));
        case CL_OUT_OF_RESOURCES:
          throw new OutOfMemoryError("No resources left");
        case CL_OUT_OF_HOST_MEMORY:
          throw new OutOfMemoryError("No more memory left");
        case CL_INVALID_VALUE:
          throw new IllegalStateException("Invalid program params");
        case CL_INVALID_BUILD_OPTIONS:
          throw new IllegalStateException("Invalid program build options");
      }
      return new Program(createCommandQueue(), programId);
    }

    private static String loadProgramSource(InputStream source) {
      try (Reader reader = new BufferedReader(new InputStreamReader(source))) {
        StringBuilder result = new StringBuilder();
        char[] buff = new char[512];
        for (int read = reader.read(buff); read > 0; read = reader.read(buff)) {
          result.append(buff);
        }
        return result.toString();
      } catch (IOException exc) {
        throw new IllegalStateException("Can not read OpenCL program source", exc);
      }
    }

    public Program createProgramWithSource(InputStream source) {
      return createProgramWithSource(loadProgramSource(source));
    }

    private String getProgramBuildInfo(int paramName) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer pp = stack.mallocPointer(1);
        validateCL(
            clGetProgramBuildInfo(this.id, this.device.getId(), paramName, (ByteBuffer) null, pp));
        int bytes = (int) pp.get(0);
        ByteBuffer buffer = stack.malloc(bytes);
        validateCL(clGetProgramBuildInfo(this.id, this.device.getId(), paramName, buffer, null));
        return MemoryUtil.memASCII(buffer, bytes - 1);
      }
    }

    @Override
    public void close() throws RuntimeException {
      validateCL(clReleaseContext(id));
    }

  }

  /**
   * OpenCL program object helper
   */
  public static final class Program implements AutoCloseable {
    private final CommandQueue cmdQueue;
    private final Set<Kernel> kernels;
    private final long id;

    private Program(CommandQueue cmdQueue, long id) {
      this.cmdQueue = cmdQueue;
      this.kernels = new TreeSet<Kernel>();
      this.id = id;
    }

    /**
     * Returns current program command queue
     * 
     * @return
     */
    public CommandQueue getCommandQueue() {
      return cmdQueue;
    }

    /**
     * Obtains OpenCL kernel from this program
     * 
     * @param name kernel name as specified in shader
     * @return kernel object
     */
    public Kernel createKernel(final String name) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = stack.mallocInt(1);
        long kernelId = clCreateKernel(id, name, err);
        if (0L != kernelId || CL_SUCCESS != err.get()) {
          switch (err.get(0)) {
            case CL_SUCCESS:
              break;
            case CL_INVALID_PROGRAM_EXECUTABLE:
              throw new IllegalStateException("No successfully built executable for program");
            case CL_INVALID_KERNEL_NAME:
              throw new IllegalStateException("Invalid kernel name");
            case CL_INVALID_KERNEL_DEFINITION:
              throw new IllegalStateException(
                  "The function definition for __kernel function given by"
                      + "kernel_name such as the number of arguments, "
                      + "the argument types are not the same for all devices for which the program executable "
                      + "has been built.");
            case CL_OUT_OF_RESOURCES:
              throw new OutOfMemoryError(
                  "Failure to allocate resources required by the OpenCL implementation on the device.");
            case CL_OUT_OF_HOST_MEMORY:
              throw new OutOfMemoryError(
                  "Failure to allocate resources required by the OpenCL implementation on the host.");
            default:
              throw new IllegalStateException("OpenCL error. Code: " + err.get());
          }
        }
        Kernel result = new Kernel(cmdQueue, kernelId);
        kernels.add(result);
        return result;
      }
    }

    @Override
    public void close() throws RuntimeException {
      for (Kernel kernel : kernels) {
        clReleaseKernel(kernel.getId());
      }
      cmdQueue.close();
      validateCL(clReleaseProgram(this.id));
    }

  }

  /**
   * OpenCL kernel object helper
   */
  public static final class Kernel implements Comparable<Kernel> {

    private final long id;

    private final CommandQueue cmdQueue;

    private int argIndex;

    private Kernel(CommandQueue cmdQueue, long id) {
      this.cmdQueue = cmdQueue;
      this.id = id;
    }

    private static void validateArg(final int errorCode) {
      switch (errorCode) {
        case CL_INVALID_ARG_INDEX:
          throw new IllegalArgumentException("Invalid argument index");
        case CL_INVALID_ARG_VALUE:
          throw new IllegalArgumentException("Non NULL value expected");
        case CL_INVALID_MEM_OBJECT:
          throw new IllegalArgumentException("Memory object expected");
        case CL_INVALID_SAMPLER:
          throw new IllegalArgumentException("Sampler expected");
        case CL_SUCCESS:
          break;
      }
    }

    private void addArg(int index, int val) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        validateArg(
            clSetKernelArg(this.getId(), index, (IntBuffer) stack.mallocInt(1).put(val).flip()));
      }
    }


    private void addArg(int index, VideoMemBuffer val) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        PointerBuffer ptrBuff = stack.mallocPointer(1).put(val.getId()).flip();
        validateArg(clSetKernelArg(this.getId(), index, ptrBuff));
      }
    }

    private void addArg(int index, float val) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        validateArg(clSetKernelArg(this.getId(), index, stack.mallocFloat(1).put(val).flip()));
      }
    }

    private void addArg(int index, double val) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        validateArg(clSetKernelArg(this.getId(), index, stack.mallocDouble(1).put(val).flip()));
      }
    }

    /**
     * Sequential add kernel argument
     * 
     * @param val argument value to bind
     */
    public Kernel arg(int value) {
      addArg(this.argIndex, value);
      ++this.argIndex;
      return this;
    }

    /**
     * Sequential add kernel argument
     * 
     * @param val argument value to bind
     */
    public final Kernel arg(final VideoMemBuffer value) {
      addArg(this.argIndex, value);
      ++this.argIndex;
      return this;
    }

    /**
     * Sequential add kernel argument
     * 
     * @param val argument value to bind
     */
    public final Kernel arg(float value) {
      addArg(this.argIndex, value);
      ++this.argIndex;
      return this;
    }

    /**
     * Sequential add kernel argument
     * 
     * @param val argument value to bind
     */
    public final Kernel arg(double value) {
      addArg(this.argIndex, value);
      ++this.argIndex;
      return this;
    }

    public final void flush() {
      this.argIndex = 0;
    }

    private void validateErrorCode(int errorCode) throws ExecutionException {
      switch (errorCode) {
        case CL_SUCCESS:
          break;
        case CL_INVALID_PROGRAM_EXECUTABLE:
          throw new ExecutionException(new IllegalStateException("Invalid program"));
        case CL_INVALID_KERNEL:
          throw new ExecutionException(new IllegalStateException("Invalid kernel"));
        case CL_INVALID_KERNEL_ARGS:
          throw new ExecutionException(new IllegalStateException("Invalid kernel args"));
        case CL_INVALID_WORK_GROUP_SIZE:
          throw new ExecutionException(new IllegalStateException("Invalid work group size"));
      }
    }

    /**
     * Executes this kernel on device as data parallel
     * 
     * @param globalWorkSize - data parallel global work size
     * @throws ExecutionException in case of OpenCL error
     */
    public void executeAsDataParallel(final long globalWorkSize) throws ExecutionException {
      validateErrorCode(clEnqueueNDRangeKernel(cmdQueue.getId(), id, 1, (PointerBuffer) null,
          MemoryStack.stackPointers(globalWorkSize), (PointerBuffer) null, (PointerBuffer) null,
          (PointerBuffer) null));
    }

    long getId() {
      return this.id;
    }

    @Override
    public int compareTo(Kernel o) {
      return Long.compare(this.id, o.id);
    }
  }


  /**
   * OpenCL command queue object helper
   */
  public static final class CommandQueue {
    private final Context context;
    private final long id;

    private final Deque<VideoMemBuffer> memBuffers;

    private CommandQueue(final Context context, long id) {
      this.context = context;
      this.id = id;
      this.memBuffers = new LinkedList<>();
    }

    long getId() {
      return id;
    }


    private VideoMemBuffer hostPtrReadBuffer(long buffer, int size) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = stack.mallocInt(1);
        long bufferId = nclCreateBuffer(this.context.getId(),
            CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR, size, buffer, MemoryUtil.memAddressSafe(err));
        if (CL_OUT_OF_HOST_MEMORY == err.get(0)) {
          throw new OutOfMemoryError("Can not allocate memory");
        }
        validateCL(err.get(0), "Can not create OpenCL memory buffer");
        VideoMemBuffer result = new VideoMemBuffer(bufferId, size);
        this.memBuffers.push(result);
        return result;
      }
    }


    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(ByteBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddressSafe(buffer), buffer.remaining());
    }

    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(ShortBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddressSafe(buffer),
          (buffer.remaining() * Short.BYTES));
    }

    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(IntBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddressSafe(buffer),
          (buffer.remaining() * Integer.BYTES));
    }

    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(LongBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddressSafe(buffer),
          (buffer.remaining() * Integer.BYTES));
    }

    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(FloatBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddressSafe(buffer),
          (buffer.remaining() * Float.BYTES));
    }

    /**
     * Creates OpenCL read only memory buffer pointing on host memory (normal RAM)
     * 
     * @param buffer a host memory buffer
     */
    public VideoMemBuffer hostPtrReadBuffer(DoubleBuffer buffer) {
      return hostPtrReadBuffer(MemoryUtil.memAddress(buffer), (buffer.remaining() * Double.BYTES));
    }

    private VideoMemBuffer createBuffer(int capacityBytes, int flags) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        IntBuffer err = stack.mallocInt(1);
        long bufferId = clCreateBuffer(this.context.getId(), flags, capacityBytes, err);
        if (CL_OUT_OF_HOST_MEMORY == err.get(0)) {
          throw new OutOfMemoryError("Can not allocate memory");
        }
        validateCL(err.get(0), "Can not create OpenCL memory buffer");
        VideoMemBuffer result = new VideoMemBuffer(bufferId, capacityBytes);
        this.memBuffers.push(result);
        return result;
      }
    }

    public VideoMemBuffer createWriteBuffer(int capacityBytes) {
      return createBuffer(capacityBytes, CL_MEM_WRITE_ONLY);
    }

    public VideoMemBuffer createReadWriteBuffer(final int capacityBytes) {
      return createBuffer(capacityBytes, CL_MEM_READ_WRITE);
    }

    public void finish() {
      if (CL_SUCCESS != clFinish(id)) {
        throw new OutOfMemoryError("failure to allocate resources");
      }
    }

    public void flush() {
      if (CL_SUCCESS != clFlush(getId())) {
        throw new OutOfMemoryError("failure to allocate resources");
      }
    }

    void readVideoMemory(VideoMemBuffer src, ByteBuffer dst) {
      validateCL(clEnqueueReadBuffer(id, src.getId(), true, 0, dst, null, null), "Invalid buffer");
    }

    void close() {
      while (!this.memBuffers.isEmpty()) {
        memBuffers.pop().free();
      }
      validateCL(clReleaseCommandQueue(id));
    }

  }

  public static final class VideoMemBuffer {

    private final long id;

    private final int capacity;

    VideoMemBuffer(final long id, final int capacity) {
      this.id = id;
      this.capacity = capacity;
    }

    public boolean isNull() {
      return 0L == id;
    }

    public long getId() {
      return this.id;
    }

    public int getCapacity() {
      return capacity;
    }

    public void free() {
      if (!isNull()) {
        clReleaseMemObject(this.id);
      }
    }

  }

}
