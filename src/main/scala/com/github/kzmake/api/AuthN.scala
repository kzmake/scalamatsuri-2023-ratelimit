package com.github.kzmake.api

import org.atnos.eff.Eff
import org.atnos.eff.syntax.all._
import zio.http.Request

trait AuthN {
  type AuthenticatedUser = String

  def authenticate[R](req: Request): Eff[R, AuthenticatedUser] = "alice".pureEff[R]
}
