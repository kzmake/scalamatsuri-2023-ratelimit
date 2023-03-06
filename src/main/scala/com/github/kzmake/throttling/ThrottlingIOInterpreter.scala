package com.github.kzmake.throttling

import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff._

trait ThrottlingIOInterpreter {
  def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[ThrottlingIO, R, U],
      mkc: _throttlingKeyCost[U],
    ): Eff[U, A]
}
