package io.github.feederiken.zocl

import zio._

import org.jocl, java.nio

/**
 * Companion object for [Pointer] with shortcut constructors.
 */
object Pointer {
  // Assumption: jocl.Pointers are effectively immutable
  private lazy val theNull: Pointer = new jocl.Pointer()

  /** Provide a Pointer to null.
   */
  def apply(): Pointer = theNull

  def apply(arr: Array[Byte]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Char]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Short]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Int]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Long]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Float]): Pointer = {
    jocl.Pointer.to(arr)
  }

  def apply(arr: Array[Double]): Pointer = {
    jocl.Pointer.to(arr)
  }

  /** Create a pointer to the given buffer.
   *
   * Note that this takes offset and position into account.
   */
  def apply(buf: nio.Buffer): Pointer = {
    jocl.Pointer.toBuffer(buf)
  }

  def apply(ptr: jocl.NativePointerObject) = {
    jocl.Pointer.to(ptr)
  }

  def apply(arr: Array[jocl.NativePointerObject]) = {
    jocl.Pointer.to(arr: _*)
  }
}
