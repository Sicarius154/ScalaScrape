package application

import cats.effect.{ExitCode, IO}
import fs2.Stream

trait ScrapingStream {
  def runForever(): IO[ExitCode]
  def run(): Stream[IO, Unit]
}
