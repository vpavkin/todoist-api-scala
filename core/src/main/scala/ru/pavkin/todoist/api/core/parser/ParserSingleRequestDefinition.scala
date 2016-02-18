package ru.pavkin.todoist.api.core.parser

import cats.{FlatMap, Functor}
import ru.pavkin.todoist.api.RawRequest
import ru.pavkin.todoist.api.core._
import ru.pavkin.todoist.api.utils.{Produce, Flattener}
import shapeless.{::, <:!<, HNil}

class ParserSingleRequestDefinition[F[_], L[_], P[_], R, Req, Base](requestFactory: RawRequest Produce Req,
                                                                    executor: RequestExecutor.Aux[Req, L, Base],
                                                                    flattener: Flattener[F, L, P],
                                                                    parser: SingleResourceParser.Aux[P, Base, R])
                                                                   (override implicit val itr: HasRawRequest[R],
                                                                    override implicit val F: Functor[L])
  extends ParsedBasedRequestDefinition[F, L, P, R, Req, Base] with SingleReadResourceDefinition[F, P, R, Base] {

  def load: L[Base] = executor.execute(requestFactory.produce(itr.rawRequest))
  def flatten(r: L[P[R]]): F[R] = flattener.flatten(r)
  def parse(r: Base): P[R] = parser.parse(r)

  def and[RR](implicit
              FM: FlatMap[P],
              NEQ: <:!<[RR, R],
              ir: HasRawRequest[RR],
              rrParser: SingleResourceParser.Aux[P, Base, RR])
  : MultipleReadResourceDefinition[F, P, RR :: R :: HNil, Base] =
    new ParserMultipleRequestDefinition[F, L, P, RR :: R :: HNil, Req, Base](
      requestFactory, executor, flattener, parser.combine(rrParser)
    )
}
