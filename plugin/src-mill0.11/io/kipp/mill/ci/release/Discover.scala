package io.kipp.mill.ci.release

private[release] object Discover {
  def apply[T] = mill.define.Discover[T]
}
