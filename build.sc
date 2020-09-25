import mill._, scalalib._, scalafmt._
import publish._

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

object core extends common with PublishModule {
  def ivyDeps = Agg(zio, jocl)

  def artifactName = "zocl-core"
  def publishVersion = "0.0.1"
  def pomSettings = PomSettings(
    description = "ZIO bindings for OpenCL",
    organization = "io.github.feederiken",
    url = "https://github.com/feederiken/zocl",
    licenses = Seq(License.`CC0-1.0`),
    versionControl = VersionControl.github("feederiken", "zocl"),
    developers = Seq(
      Developer("feederiken9", "Feederiken Nine", "https://github.com/feederiken9"),
    ),
  )
}

object demo extends common {
  def moduleDeps = Seq(core)
  def ivyDeps = Agg(zio, zioNioCore)
}
