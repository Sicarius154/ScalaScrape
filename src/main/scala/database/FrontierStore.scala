package database

import cats.data.{OptionT, EitherT}
import cats.effect.IO
import domain.FrontierTarget

import java.util.UUID

trait FrontierStore {
  def getFrontierByJobID(id: UUID): IO[Set[ResourceURI]]
  def popFromFrontierForJobID(id: UUID): EitherT[IO, String, FrontierTarget]
  def pushToFrontierForJobID(id: UUID, value: String): IO[Unit]
  def removeFrontierForJobID(id: UUID): IO[Unit]
}
