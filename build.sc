import mill._, scalalib._, scalafmt._

object Deps {
  val zioVersion = "1.0.1"

  val zio = ivy"dev.zio::zio:$zioVersion"
  val zioNioCore = ivy"dev.zio::zio-nio-core:1.0.0-RC9"
  val jocl = ivy"org.jocl:jocl:2.0.2"
}

import Deps._

trait common extends ScalaModule with ScalafmtModule {
  def scalaVersion = "2.13.2"
}

object core extends common {
  def ivyDeps = Agg(zio, jocl)
}

object demo extends common {
  def moduleDeps = Seq(core)
  def ivyDeps = Agg(zio, zioNioCore)
}
