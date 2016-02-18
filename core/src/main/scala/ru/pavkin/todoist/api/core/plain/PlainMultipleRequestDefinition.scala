package ru.pavkin.todoist.api.core.plain

import cats.{FlatMap, Id}
import ru.pavkin.todoist.api.RawRequest
import ru.pavkin.todoist.api.core.parser.SingleResourceParser
import ru.pavkin.todoist.api.core.{HasRawRequest, MultipleReadResourceDefinition, RequestExecutor}
import ru.pavkin.todoist.api.utils.{NotContains, Produce}
import shapeless.{::, HList}

class PlainMultipleRequestDefinition[F[_], R <: HList, Req, Base](requestFactory: RawRequest Produce Req,
                                                                  executor: RequestExecutor.Aux[Req, F, Base])
                                                                 (override implicit val itr: HasRawRequest[R])
  extends MultipleReadResourceDefinition[F, Id, R, Base] {

  type Out = Base

  def and[RR](implicit
              F: FlatMap[Id],
              NC: NotContains[R, RR],
              ir: HasRawRequest[RR],
              parser: SingleResourceParser.Aux[Id, Base, RR]): MultipleReadResourceDefinition[F, Id, RR :: R, Base] =
    new PlainMultipleRequestDefinition[F, RR :: R, Req, Base](requestFactory, executor)

  def execute: F[Out] = executor.execute(requestFactory.produce(itr.rawRequest))
}
