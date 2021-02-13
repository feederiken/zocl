package io.github.feederiken.zocl

import zio._, zio.blocking.Blocking

trait Service extends Serializable {
  def getPlatformIds: IO[CLException, Chunk[PlatformId]]
  def getDeviceIds(
      platform: PlatformId,
      deviceType: Long,
  ): IO[CLException, Chunk[DeviceId]]
  def createContext(
      props: ContextProperties,
      devices: Seq[DeviceId],
  ): Managed[CLException, Context]
  def createCommandQueue(
      ctx: Context,
      device: DeviceId,
  ): Managed[CLException, CommandQueue]
  def createProgramWithSource(
      ctx: Context,
      programSource: Seq[String],
  ): Managed[CLException, Program]
  def createBuffer(
      ctx: Context,
      flags: Long,
      size: Long,
      ptr: Option[Pointer],
  ): Managed[CLException, MemObject]
  def createKernel(
      prog: Program,
      kernelName: String,
  ): Managed[CLException, Kernel]
  def enqueueReadBuffer(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ): Managed[CLException, Event]
  def enqueueReadBuffer_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ): IO[CLException, Unit]
  def enqueueReadBufferBlocking(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ): ZManaged[Blocking, CLException, Event]
  def enqueueReadBufferBlocking_(
      q: CommandQueue,
      buffer: MemObject,
      offset: Long,
      count: Long,
      ptr: Pointer,
      waitList: Seq[Event],
  ): ZIO[Blocking, CLException, Unit]
  def enqueueNDRangeKernel(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]],
      globalWorkSize: Seq[Long],
      localWorkSize: Option[Seq[Long]],
      waitList: Seq[Event],
  ): Managed[CLException, Event]
  def enqueueNDRangeKernel_(
      q: CommandQueue,
      kernel: Kernel,
      globalWorkOffset: Option[Seq[Long]],
      globalWorkSize: Seq[Long],
      localWorkSize: Option[Seq[Long]],
      waitList: Seq[Event],
  ): IO[CLException, Unit]
  def buildProgram(
      prog: Program,
      devices: Seq[DeviceId],
      options: String,
  ): IO[CLException, Unit]
  def buildProgramBlocking(
      prog: Program,
      devices: Seq[DeviceId],
      options: String,
  ): ZIO[Blocking, CLException, Unit]
  def setKernelArgs(
      kernel: Kernel,
      arg_index: Int,
      arg_size: Long,
      arg_value: Pointer,
  ): IO[CLException, Unit]
  def waitForEvent(event: Event): IO[CLException, Unit]
  def waitForEventsBlocking(
      events: Seq[Event]
  ): ZIO[Blocking, CLException, Unit]
  def retainKernel(kernel: Kernel): Managed[CLException, Kernel]
  def releaseKernelUnsafe(kernel: Kernel): IO[CLException, Unit]
  def retainCommandQueue(
      queue: CommandQueue
  ): Managed[CLException, CommandQueue]
  def releaseCommandQueueUnsafe(queue: CommandQueue): IO[CLException, Unit]
  def retainMemObject(mem: MemObject): Managed[CLException, MemObject]
  def releaseMemObjectUnsafe(mem: MemObject): IO[CLException, Unit]
  def retainProgram(prog: Program): Managed[CLException, Program]
  def releaseProgramUnsafe(prog: Program): IO[CLException, Unit]
  def retainContext(ctx: Context): Managed[CLException, Context]
  def releaseContextUnsafe(ctx: Context): IO[CLException, Unit]
  def retainEvent(event: Event): Managed[CLException, Event]
  def releaseEventUnsafe(event: Event): IO[CLException, Unit]
}
