package io.github.feederiken

import zio._
import zio.blocking.Blocking

/**
  * Unopinionated ZIO bindings for [OpenCL](https://www.khronos.org/opencl/).
  *
  * It's type-safe, resource safe and asynchronous-enabled, but won't check for buffer overflows or endianness.
  *
  * Note: the bindings disable JOCL's exception throwing feature and work best when it stays disabled.
  *
  * @define releaseUnsafe In normal use, it is redundant to call this operation directly because zocl resources are managed through ZManaged.
  */

package object zocl {
  type CL = Has[Service]

  type Program = org.jocl.cl_program
  type Pointer = org.jocl.Pointer
  type PlatformId = org.jocl.cl_platform_id
  type Kernel = org.jocl.cl_kernel
  type Event = org.jocl.cl_event
  type DeviceId = org.jocl.cl_device_id
  type Context = org.jocl.cl_context
  type ContextProperties = org.jocl.cl_context_properties
  type CommandQueue = org.jocl.cl_command_queue
  type MemObject = org.jocl.cl_mem
  type CLException = org.jocl.CLException

  val getPlatformIds: ZIO[CL, CLException, Chunk[PlatformId]] =
    ZIO.service[Service] >>= { _.getPlatformIds }
  def getDeviceIds(
      platform: PlatformId,
      deviceType: Long,
  ): ZIO[CL, CLException, Chunk[DeviceId]] =
    ZIO.service[Service] >>= { _.getDeviceIds(platform, deviceType) }
  def createContext(
      props: ContextProperties,
      devices: Seq[DeviceId],
  ): ZManaged[CL, CLException, Context] =
    ZManaged.service[Service] >>= { _.createContext(props, devices) }
  def createCommandQueue(
      ctx: Context,
      device: DeviceId,
  ): ZManaged[CL, CLException, CommandQueue] =
    ZManaged.service[Service] >>= { _.createCommandQueue(ctx, device) }
  def createProgramWithSource(
      ctx: Context,
      programSource: Seq[String],
  ): ZManaged[CL, CLException, Program] =
    ZManaged.service[Service] >>= {
      _.createProgramWithSource(ctx, programSource)
    }
  def createProgramWithSource(
      ctx: Context,
      programSource: String,
  ): ZManaged[CL, CLException, Program] =
    createProgramWithSource(ctx, Seq(programSource))
  def createBuffer(
      ctx: Context,
      flags: Long,
      size: Long,
      ptr: Option[Pointer] = None,
  ): ZManaged[CL, CLException, MemObject] =
    ZManaged.service[Service] >>= { _.createBuffer(ctx, flags, size, ptr) }
  def createKernel(
      prog: Program,
      kernelName: String,
  ): ZManaged[CL, CLException, Kernel] =
    ZManaged.service[Service] >>= { _.createKernel(prog, kernelName) }
  def enqueueReadBuffer(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event] = Nil,
  ): ZManaged[CL, CLException, Event] =
    ZManaged.service[Service] >>= {
      _.enqueueReadBuffer(q, buffer, offset, count, ptr, waitList)
    }
  def enqueueReadBuffer_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event] = Nil,
  ): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= {
      _.enqueueReadBuffer_(q, buffer, offset, count, ptr, waitList)
    }
  def enqueueReadBufferBlocking(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event] = Nil,
  ): ZManaged[CL with Blocking, CLException, Event] =
    ZManaged.service[Service] >>= {
      _.enqueueReadBufferBlocking(q, buffer, offset, count, ptr, waitList)
    }
  def enqueueReadBufferBlocking_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event] = Nil,
  ): ZIO[CL with Blocking, CLException, Unit] =
    ZIO.service[Service] >>= {
      _.enqueueReadBufferBlocking_(q, buffer, offset, count, ptr, waitList)
    }

  def enqueueNDRangeKernel(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]] = None,
      globalWorkSize: Seq[Long] = List(1),
      localWorkSize: Option[Seq[Long]] = None,
      waitList: Seq[Event] = Nil,
  ): ZManaged[CL, CLException, Event] =
    ZManaged.service[Service] >>= {
      _.enqueueNDRangeKernel(
        q,
        kernel,
        globalWorkOffset,
        globalWorkSize,
        localWorkSize,
        waitList,
      )
    }

  /**
    * Build (compiles and links) a CL program executable from the program source or binary.
    *
    * The effect waits for completion asynchronously using a callback.
    *
    * Note: Due to the design of the underlying callback interface, unlike
    * [buildProgramBlocking], the success of this operation does not guarantee
    * that the program passed compilation. It only guarantees that the
    * compilation is no longer in progress. Make sure to check the build status
    * on the program object, or use [buildProgramBlocking].
    */
  def buildProgram(
      prog: Program,
      devices: Seq[DeviceId],
      options: String = "",
  ): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.buildProgram(prog, devices, options) }

  /**
    * Build (compiles and links) a CL program executable from the program source or binary.
    *
    * The effect waits for completion by blocking synchronously.
    */
  def buildProgramBlocking(
      prog: Program,
      devices: Seq[DeviceId],
      options: String = "",
  ): ZIO[CL with Blocking, CLException, Unit] =
    ZIO.service[Service] >>= { _.buildProgramBlocking(prog, devices, options) }

  def setKernelArgs(
      kernel: Kernel,
      arg_index: Int,
      arg_size: Long,
      arg_value: Pointer,
  ): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= {
      _.setKernelArgs(kernel, arg_index, arg_size, arg_value)
    }

  /**
    * Asynchronously wait for an event to complete.
    *
    * Uses [[org.jocl.CL.clSetEventCallback]].
    */
  def waitForEvent(event: Event): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.waitForEvent(event) }

  /**
    * Asynchronously wait for some events to complete.
    *
    * Uses [[org.jocl.CL.clSetEventCallback]].
    */
  def waitForEvent(events: Event*): ZIO[CL, CLException, Unit] =
    waitForEvents(events.toSeq)

  /**
    * Asynchronously wait for some events to complete.
    *
    * Uses [[org.jocl.CL.clSetEventCallback]].
    */
  def waitForEvents(events: Seq[Event]): ZIO[CL, CLException, Unit] =
    ZIO.foreachPar_(events) { waitForEvent(_) }

  /**
    * Synchronously block on some events.
    *
    * Uses [[org.jocl.CL.clWaitForEvents]].
    */
  def waitForEventsBlocking(
      events: Seq[Event]
  ): ZIO[CL with Blocking, CLException, Unit] =
    ZIO.service[Service] >>= { _.waitForEventsBlocking(events) }

  def retainContext(ctx: Context): ZManaged[CL, CLException, Context] =
    ZManaged.service[Service] >>= { _.retainContext(ctx) }

  /**
    * Release the resources associated with the given context directly.
    *
    * $releaseUnsafe
    */
  def releaseContextUnsafe(ctx: Context): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.releaseContextUnsafe(ctx) }

  def retainCommandQueue(
      queue: CommandQueue
  ): ZManaged[CL, CLException, CommandQueue] =
    ZManaged.service[Service] >>= { _.retainCommandQueue(queue) }

  /**
    * Release the resources associated with the given command queue directly.
    *
    * $releaseUnsafe
    */
  def releaseCommandQueueUnsafe(
      queue: CommandQueue
  ): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.releaseCommandQueueUnsafe(queue) }

  def retainMemObject(mem: MemObject): ZManaged[CL, CLException, MemObject] =
    ZManaged.service[Service] >>= { _.retainMemObject(mem) }

  /**
    * Release the resources associated with the given memory object directly.
    *
    * $releaseUnsafe
    */
  def releaseMemObjectUnsafe(mem: MemObject): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.releaseMemObjectUnsafe(mem) }

  def retainProgram(prog: Program): ZManaged[CL, CLException, Program] =
    ZManaged.service[Service] >>= { _.retainProgram(prog) }

  /**
    * Release the resources associated with the given program directly.
    *
    * $releaseUnsafe
    */
  def releaseProgramUnsafe(prog: Program): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.releaseProgramUnsafe(prog) }

  def retainEvent(event: Event): ZManaged[CL, CLException, Event] =
    ZManaged.service[Service] >>= { _.retainEvent(event) }

  /**
    * Release the resources associated with the given event directly.
    *
    * $releaseUnsafe
    */
  def releaseEventUnsafe(event: Event): ZIO[CL, CLException, Unit] =
    ZIO.service[Service] >>= { _.releaseEventUnsafe(event) }
}
