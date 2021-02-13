package io.github.feederiken.zocl.demo

import zio._, zio.blocking._, zio.nio.core._
import org.jocl.CL._, org.jocl.Sizeof
import io.github.feederiken.zocl._
import java.io.EOFException

object Demo extends App {
  val readProgram: RIO[Blocking, String] =
    for {
      progPath <- IO(getClass.getClassLoader.getResource("opencl/add2.cl"))
      bytes <- Managed.readURL(progPath).use {
        _.readAll(4096).orDieWith(_.getOrElse(new java.io.EOFException))
      }
    } yield new String(bytes.toArray)

  def program(scope: Managed.Scope) = {
    def lift[R, E, A](resource: ZManaged[R, E, A]) =
      scope(resource) >>> ZIO.second
    for {
      p <- getPlatformIds.mapEffect(_.head)
      ds <- getDeviceIds(p, CL_DEVICE_TYPE_ALL)
      prog <- readProgram
      ctx <- lift(createContext(new ContextProperties, ds))
      q <- lift(createCommandQueue(ctx, ds.head))
      prog <- lift(createProgramWithSource(ctx, prog))
      _ <- buildProgram(prog, ds)
      k <- lift(createKernel(prog, "sampleKernel"))
      two = Pointer(Array(2f))
      bufA <- lift(
        createBuffer(
          ctx,
          CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
          Sizeof.cl_float,
          Some(two),
        )
      )
      bufB <- lift(
        createBuffer(
          ctx,
          CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
          Sizeof.cl_float,
          Some(two),
        )
      )
      bufC <- lift(createBuffer(ctx, CL_MEM_WRITE_ONLY, Sizeof.cl_float))
      _ <- setKernelArgs(k, 0, Sizeof.cl_mem, Pointer(bufA))
      _ <- setKernelArgs(k, 1, Sizeof.cl_mem, Pointer(bufB))
      _ <- setKernelArgs(k, 2, Sizeof.cl_mem, Pointer(bufC))
      _ <- enqueueNDRangeKernel(q, k).use(waitForEvent)
      dest <- Buffer.byteDirect(Sizeof.cl_float)
      _ <- dest.order(java.nio.ByteOrder.nativeOrder)
      destptr <- dest.withJavaBuffer(b => UIO(Pointer(b)))
      _ <- enqueueReadBuffer(q, bufC, 0, Sizeof.cl_float, destptr)
        .use(waitForEvent)
      r <- dest.getFloat
      _ <- console.putStrLn(s"2 + 2 = $r")
    } yield ()
  }
  def run(args: List[String]) =
    Managed.scope.use(program(_).provideCustomLayer(CL.live).exitCode)
}
