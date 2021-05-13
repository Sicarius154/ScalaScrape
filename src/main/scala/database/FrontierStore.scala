package database

import cats.data.{OptionT, EitherT}
import cats.effect.IO
import domain.FrontierTarget

import java.util.UUID

trait FrontierStore {
  def getFrontierByJobID(id: UUID): IO[Set[ResourceURI]]
  def popRandomFromFrontierForJobID(id: UUID): OptionT[IO, FrontierTarget]
  def pushToFrontierForJobID(id: UUID, value: String): IO[Unit]

  //TODO: Determine whether this is needed. popRandomFromFrontierForJobID() fulfills the job of this function
  //The only reason we will need this is if we need to remove items in a specified order
  def removeFrontierForJobID(id: UUID): IO[Unit]
}
