package io.github.feederiken.zocl

import zio._

import org.jocl._, CL._

private object Implementation extends Service {
  setExceptionsEnabled(false)

  def checkResult(result: Int): IO[CLException, Unit] = {
    if (result < 0)
      IO.fail(new CLException(stringFor_errorCode(result), result))
    else
      IO.unit
  }

  type Callback = IO[CLException, Unit] => Unit

  object CallbackAdapter
      extends EventCallbackFunction
      with BuildProgramFunction {
    override def function(prog: Program, userdata: Object): Unit = {
      val cb = userdata.asInstanceOf[Callback]
      cb(IO.unit)
    }

    override def function(
        event: Event,
        command_exec_callback_type: Int,
        userdata: Object,
    ): Unit = {
      val cb = userdata.asInstanceOf[Callback]
      cb(checkResult(command_exec_callback_type))
    }
  }

  private def nullIfEmpty[A](a: Array[A]): Array[A] =
    // CL functions don't like empty arrays
    if (a.isEmpty) null else a

  override def retainKernel(kernel: Kernel): Managed[CLException, Kernel] =
    ZManaged.make {
      IO.effectSuspendTotal {
        val result = clRetainKernel(kernel)
        checkResult(result).as(kernel)
      }
    } {
      releaseKernelUnsafe(_).orDie
    }

  override def retainMemObject(
      mem: MemObject
  ): Managed[CLException, MemObject] =
    ZManaged.make {
      IO.effectSuspendTotal {
        val result = clRetainMemObject(mem)
        checkResult(result).as(mem)
      }
    } {
      releaseMemObjectUnsafe(_).orDie
    }

  override def retainProgram(prog: Program): Managed[CLException, Program] =
    ZManaged.make {
      IO.effectSuspendTotal {
        val result = clRetainProgram(prog)
        checkResult(result).as(prog)
      }
    } {
      releaseProgramUnsafe(_).orDie
    }

  override def retainContext(ctx: Context): Managed[CLException, Context] =
    ZManaged.make {
      IO.effectSuspendTotal {
        val result = clRetainContext(ctx)
        checkResult(result).as(ctx)
      }
    } {
      releaseContextUnsafe(_).orDie
    }

  override def retainEvent(event: Event): Managed[CLException, Event] =
    ZManaged.make {
      IO.effectSuspendTotal {
        val result = clRetainEvent(event)
        checkResult(result).as(event)
      }
    } {
      releaseEventUnsafe(_).orDie
    }

  def getPlatformIds =
    IO.effectSuspendTotal {
      val num_platforms = Array(0)
      checkResult(clGetPlatformIDs(0, null, num_platforms)) *> {
        val platforms = Array.ofDim[cl_platform_id](num_platforms(0))
        val result =
          clGetPlatformIDs(platforms.length, platforms, num_platforms)
        checkResult(result).as(
          Chunk.fromArray(platforms).take(num_platforms(0))
        )
      }
    }

  def getDeviceIds(platform: PlatformId, deviceType: Long) =
    IO.effectSuspendTotal {
      val num_devices = Array(0)
      checkResult(
        clGetDeviceIDs(platform, deviceType, 0, null, num_devices)
      ) *> {
        val devices = Array.ofDim[cl_device_id](num_devices(0))
        val result = clGetDeviceIDs(
          platform,
          deviceType,
          devices.length,
          devices,
          num_devices,
        )
        checkResult(result).as(Chunk.fromArray(devices).take(num_devices(0)))
      }
    }

  override def releaseContextUnsafe(ctx: Context) =
    IO.effectSuspendTotal {
      checkResult(clReleaseContext(ctx))
    }

  override def releaseCommandQueueUnsafe(q: CommandQueue) =
    IO.effectSuspendTotal {
      checkResult(clReleaseCommandQueue(q))
    }

  override def releaseProgramUnsafe(prog: Program) =
    IO.effectSuspendTotal {
      checkResult(clReleaseProgram(prog))
    }
  override def releaseMemObjectUnsafe(mem: MemObject) =
    IO.effectSuspendTotal {
      checkResult(clReleaseMemObject(mem))
    }
  override def releaseKernelUnsafe(k: Kernel) =
    IO.effectSuspendTotal {
      checkResult(clReleaseKernel(k))
    }
  override def releaseEventUnsafe(event: Event) =
    IO.effectSuspendTotal {
      checkResult(clReleaseEvent(event))
    }

  def createContext(props: ContextProperties, devices: Seq[DeviceId]) =
    Managed.make {
      IO.effectSuspendTotal {
        val result = Array.ofDim[Int](1)
        val devicesA = devices.toArray
        val ctx =
          clCreateContext(props, devicesA.length, devicesA, null, null, result)
        checkResult(result(0)).as(ctx)
      }
    } {
      releaseContextUnsafe(_).orDie
    }

  def createCommandQueue(ctx: Context, device: DeviceId) =
    Managed.make {
      IO.effectSuspendTotal {
        val result = Array.ofDim[Int](1)
        val q = clCreateCommandQueueWithProperties(ctx, device, null, result)
        checkResult(result(0)).as(q)
      }
    } {
      releaseCommandQueueUnsafe(_).orDie
    }

  def createProgramWithSource(ctx: Context, programSource: Seq[String]) =
    Managed.make {
      IO.effectSuspendTotal {
        val programSourceA = programSource.toArray
        val result = Array.ofDim[Int](1)
        val prog = clCreateProgramWithSource(
          ctx,
          programSourceA.length,
          programSourceA,
          null,
          result,
        )
        checkResult(result(0)).as(prog)
      }
    } {
      releaseProgramUnsafe(_).orDie
    }

  def createBuffer(
      ctx: Context,
      flags: Long,
      size: Long,
      ptr: Option[Pointer],
  ) =
    Managed.make {
      IO.effectSuspendTotal {
        val result = Array.ofDim[Int](1)
        val buf = clCreateBuffer(ctx, flags, size, ptr.orNull, result)
        checkResult(result(0)).as(buf)
      }
    } {
      releaseMemObjectUnsafe(_).orDie
    }

  def createKernel(prog: Program, kernelName: String) =
    Managed.make {
      IO.effectSuspendTotal {
        val result = Array.ofDim[Int](1)
        val k = clCreateKernel(prog, kernelName, null)
        checkResult(result(0)).as(k)
      }
    } {
      releaseKernelUnsafe(_).orDie
    }

  private val makeEvent =
    Managed.make {
      IO.effectTotal(new Event)
    } {
      releaseEventUnsafe(_).orDie
    }

  private def enqueueReadBufferImpl(
      q: CommandQueue,
      buffer: MemObject,
      blockingRead: Boolean,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
      event: Event,
  ) =
    IO.effectSuspendTotal {
      val waitListA = waitList.toArray
      val result = clEnqueueReadBuffer(
        q,
        buffer,
        blockingRead,
        offset,
        count,
        ptr,
        waitListA.length,
        nullIfEmpty(waitListA),
        event,
      )
      checkResult(result)
    }

  def enqueueReadBuffer(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    makeEvent.tapM {
      enqueueReadBufferImpl(q, buffer, false, offset, count, ptr, waitList, _)
    }

  def enqueueReadBuffer_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    enqueueReadBufferImpl(q, buffer, false, offset, count, ptr, waitList, null)

  def enqueueReadBufferBlocking(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    makeEvent.tapM { event =>
      blocking.blocking {
        enqueueReadBufferImpl(
          q,
          buffer,
          true,
          offset,
          count,
          ptr,
          waitList,
          event,
        )
      }
    }

  def enqueueReadBufferBlocking_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    blocking.blocking {
      enqueueReadBufferImpl(q, buffer, true, offset, count, ptr, waitList, null)
    }

  private def enqueueWriteBufferImpl(
      q: CommandQueue,
      buffer: MemObject,
      blockingWrite: Boolean,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
      event: Event,
  ) =
    IO.effectSuspendTotal {
      val waitListA = waitList.toArray
      val result = clEnqueueWriteBuffer(
        q,
        buffer,
        blockingWrite,
        offset,
        count,
        ptr,
        waitListA.length,
        nullIfEmpty(waitListA),
        event,
      )
      checkResult(result)
    }

  def enqueueWriteBuffer(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    makeEvent.tapM {
      enqueueWriteBufferImpl(q, buffer, false, offset, count, ptr, waitList, _)
    }

  def enqueueWriteBuffer_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    enqueueWriteBufferImpl(q, buffer, false, offset, count, ptr, waitList, null)

  def enqueueWriteBufferBlocking(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    makeEvent.tapM { event =>
      blocking.blocking {
        enqueueWriteBufferImpl(
          q,
          buffer,
          true,
          offset,
          count,
          ptr,
          waitList,
          event,
        )
      }
    }

  def enqueueWriteBufferBlocking_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ) =
    blocking.blocking {
      enqueueWriteBufferImpl(q, buffer, true, offset, count, ptr, waitList, null)
    }

  private def enqueueNDRangeKernelImpl(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]],
      globalWorkSize: Seq[Long],
      localWorkSize: Option[Seq[Long]],
      waitList: Seq[Event],
      event: Event,
  ) =
    IO.effectSuspendTotal {
      val globalWorkOffsetA = globalWorkOffset.map(_.toArray)
      val globalWorkSizeA = globalWorkSize.toArray
      val localWorkSizeA = localWorkSize.map(_.toArray)
      val workDim = globalWorkSizeA.length
      globalWorkOffsetA.foreach { a =>
        require(
          a.length == workDim,
          "globalWorkOffset has inconsistent length",
        )
      }
      localWorkSizeA.foreach { a =>
        require(a.length == workDim, "localWorkSize has inconsistent length")
      }
      val waitListA = waitList.toArray
      val result = clEnqueueNDRangeKernel(
        q,
        kernel,
        globalWorkSizeA.length,
        globalWorkOffsetA.orNull,
        globalWorkSizeA,
        localWorkSizeA.orNull,
        waitListA.length,
        nullIfEmpty(waitListA),
        event,
      )
      checkResult(result)
    }
  def enqueueNDRangeKernel(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]],
      globalWorkSize: Seq[Long],
      localWorkSize: Option[Seq[Long]],
      waitList: Seq[Event],
  ) =
    makeEvent.tapM {
      enqueueNDRangeKernelImpl(
        q,
        kernel,
        globalWorkOffset,
        globalWorkSize,
        localWorkSize,
        waitList,
        _,
      )
    }
  def enqueueNDRangeKernel_(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]],
      globalWorkSize: Seq[Long],
      localWorkSize: Option[Seq[Long]],
      waitList: Seq[Event],
  ) =
    enqueueNDRangeKernelImpl(
      q,
      kernel,
      globalWorkOffset,
      globalWorkSize,
      localWorkSize,
      waitList,
      null,
    )

  def waitForEvent(event: Event) =
    IO.effectAsync { (cb: Callback) =>
      val result = clSetEventCallback(event, CL_SUCCESS, CallbackAdapter, cb)
      if (result < 0) cb(checkResult(result))
    }

  def buildProgram(prog: Program, devices: Seq[DeviceId], options: String) =
    IO.effectAsync { (cb: Callback) =>
      val devicesA = devices.toArray
      val result = clBuildProgram(
        prog,
        devicesA.length,
        devicesA,
        options,
        CallbackAdapter,
        cb,
      )
      if (result < 0) cb(checkResult(result))
    }

  def buildProgramBlocking(
      prog: Program,
      devices: Seq[DeviceId],
      options: String,
  ) =
    blocking.blocking {
      IO.effectSuspendTotal {
        val devicesA = devices.toArray
        val result =
          clBuildProgram(prog, devicesA.length, devicesA, options, null, null)
        checkResult(result)
      }
    }

  def setKernelArgs(
      kernel: Kernel,
      arg_index: Int,
      arg_size: Long,
      arg_value: Pointer,
  ) =
    IO.effectSuspendTotal {
      val result = clSetKernelArg(kernel, arg_index, arg_size, arg_value)
      checkResult(result)
    }

  def waitForEventsBlocking(events: Seq[Event]) =
    blocking.blocking {
      IO.effectSuspendTotal {
        val eventsA = events.toArray
        checkResult(clWaitForEvents(eventsA.length, eventsA))
      }
    }

  override def retainCommandQueue(queue: CommandQueue) =
    ZManaged.make {
      IO.effectSuspendTotal {
        checkResult(clRetainCommandQueue(queue)).as(queue)
      }
    } {
      releaseCommandQueueUnsafe(_).orDie
    }
}
