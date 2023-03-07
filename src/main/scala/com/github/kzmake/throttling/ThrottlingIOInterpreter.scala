package com.github.kzmake.throttling

import com.github.kzmake.kvstore.KVStoreIOTypes._kvstoreio
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff._
import org.atnos.eff.all._

trait ThrottlingIOInterpreter {
  def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[ThrottlingIO, R, U],
      ms: _stateKeyCost[U],
      mk: _kvstoreio[U],
      me: _throwableEither[U],
    ): Eff[U, A]
}
