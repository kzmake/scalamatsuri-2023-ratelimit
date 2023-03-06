package com.github.kzmake.throttling

import com.github.kzmake.throttling.ThrottlingIO._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff.Eff

trait ThrottlingIOCreation {
  def use[R: _throttlingio](bucket: Key, request: Cost): Eff[R, Unit] = Eff.send[ThrottlingIO, R, Unit](Use(bucket, request))
  def throttle[R: _throttlingio]: Eff[R, Unit]                  = Eff.send[ThrottlingIO, R, Unit](Throttle())
}
