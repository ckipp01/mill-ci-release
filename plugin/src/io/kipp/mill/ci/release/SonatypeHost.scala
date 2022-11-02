package io.kipp.mill.ci.release

sealed trait SonatypeHost
object SonatypeHost {
  case object Legacy extends SonatypeHost
  case object s01 extends SonatypeHost
}
