package application

import cats.data.EitherT
import fs2.kafka.{ConsumerSettings, AutoOffsetReset, CommittableConsumerRecord, KafkaConsumer}
import cats.effect.{ContextShift, Async, Timer, ExitCode, IO, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import config.StreamConfig
import database.FrontierStore
import domain.TargetSeed
import fs2.Stream
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Uri.RegName
import org.http4s.{Query, Uri}
import org.http4s.client.Client
import org.http4s.client._
import org.slf4j.{Logger, LoggerFactory}
import web.WebResourceRetriever

import java.util.UUID
import scala.concurrent.duration.DurationInt

class ParallelScrapingStream(
    streamConfig: StreamConfig,
    HTTPClient: Client[IO],
    frontierStore: FrontierStore,
    webResourceRetriever: WebResourceRetriever
)(implicit
    cs: ContextShift[IO],
    timer: Timer[IO],
    sync: Sync[IO],
    async: Async[IO]
) extends ScrapingStream {
  private val log: Logger =
    LoggerFactory.getLogger(getClass.getSimpleName)

  val consumerSettings: ConsumerSettings[IO, String, String] =
    ConsumerSettings[IO, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Latest)
      .withBootstrapServers(streamConfig.bootstrapServer)
      .withGroupId(streamConfig.consumerGroup)

  override def runForever(): IO[ExitCode] =
    run().compile.drain.as(ExitCode.Success)

  //TODO: Load concurrency value from pure config
  override def run(): Stream[IO, Unit] =
    KafkaConsumer
      .stream(consumerSettings)
      .evalTap(_.subscribeTo(streamConfig.seedTopic))
      .flatMap(_.stream)
      .mapAsync(20)(processKafkaRecord)
      .unNone
      .mapAsync(20)(target => webResourceRetriever.retrieveResource(target.seedURL, 1000))
      .parEvalMap(20)(_ => IO.unit)

  private[application] def processKafkaRecord(
      record: CommittableConsumerRecord[IO, String, String]
  ): IO[Option[TargetSeed]] =
    decode[TargetSeed](record.record.value) match {
      case Right(target) => IO.pure(Some(target))
      case Left(e) => {
        IO(
          log.error(
            s"Error serializing Kafka record to ScrapeTarget. Error: ${e.getMessage}"
          )
        ) >> IO.pure(None)
      }
    }
}

object ParallelScrapingStream {
  def apply(
      streamConfig: StreamConfig,
      HTTPClient: Client[IO],
      frontierStore: FrontierStore,
      webResourceRetriever: WebResourceRetriever
  )(implicit
      cs: ContextShift[IO],
      timer: Timer[IO],
      sync: Sync[IO],
      async: Async[IO]
  ): ParallelScrapingStream =
    new ParallelScrapingStream(streamConfig, HTTPClient, frontierStore, webResourceRetriever)
}
