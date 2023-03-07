package com.github.kzmake.kvstore

import com.github.kzmake.kvstore.KVStoreIOTypes._
import org.atnos.eff._

trait KVStoreIOInterpreter {
  def run[R, U, A](effects: Eff[R, A])(implicit m: Member.Aux[KVStoreIO, R, U], ms: _stateKeyValue[U]): Eff[U, A]
}
