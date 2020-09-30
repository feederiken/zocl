package io.github.feederiken.zocl

import zio._

object CL {

  /**
    * Prepare the bindings for use.
    *
    * May fail the OpenCL native library cannot be found.
    */
  def live: Layer[UnsatisfiedLinkError, CL] =
    ZLayer.fromEffect(
      IO(new Implementation).refineToOrDie[UnsatisfiedLinkError]
    )
  def any: URLayer[CL, CL] = ZLayer.service
}
