package com.github.kzmake.api

import org.atnos.eff.Eff
import org.atnos.eff.syntax.all._
import zio.http.Request

trait RequestExtractor {
  type User      = String
  type IpAddress = String

  @scala.annotation.nowarn
  def authenticate[R](req: Request): Eff[R, User] = "alice".pureEff[R]

  @scala.annotation.nowarn
  def ipAddress[R](req: Request): Eff[R, IpAddress] = "192.0.2.0".pureEff[R]
}
