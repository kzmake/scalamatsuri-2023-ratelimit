package com.github.kzmake.throttling

import com.github.kzmake.throttling.ThrottlingIO._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff.Eff

trait ThrottlingIOCreation {
  def use[R: _throttlingio](scope: String, bucket: String, cost: Int): Eff[R, Unit] = Eff.send[ThrottlingIO, R, Unit](Use(s"/$scope/$bucket", cost))
  def use[R: _throttlingio](bucket: String, cost: Int): Eff[R, Unit]                = Eff.send[ThrottlingIO, R, Unit](Use(s"/-/$bucket", cost))
  def throttle[R: _throttlingio]: Eff[R, Unit]                                      = Eff.send[ThrottlingIO, R, Unit](Throttle())
}
