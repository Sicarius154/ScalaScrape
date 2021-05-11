package application

import fs2.kafka.{
  ConsumerSettings,
  AutoOffsetReset,
  CommittableConsumerRecord,
  KafkaConsumer
}
import cats.effect.{ContextShift, Async, Timer, ExitCode, IO, Sync}
import cats.implicits.catsSyntaxFlatMapOps
import config.StreamConfig
import domain.ScrapeTarget
import fs2.Stream
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.{Logger, LoggerFactory}

class ParallelScrapingStream(
    streamConfig: StreamConfig
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
      .mapAsync(20)(i => IO(log.info(s"Got target ${i.seedURL}")))

  private[application] def processKafkaRecord(
      record: CommittableConsumerRecord[IO, String, String]
  ): IO[Option[ScrapeTarget]] = {
    log.info(s"Here")
    decode[ScrapeTarget](record.record.value) match {
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
}

object ParallelScrapingStream {
  def apply(streamConfig: StreamConfig)(implicit
      cs: ContextShift[IO],
      timer: Timer[IO],
      sync: Sync[IO],
      async: Async[IO]
  ): ParallelScrapingStream = new ParallelScrapingStream(streamConfig)
}
