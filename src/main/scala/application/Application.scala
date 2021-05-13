package application

import config.{Config, FrontierConfig}
import cats.syntax._
import cats.implicits._
import cats.effect.{Timer, IO, ExitCode, ContextShift}
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import cats.effect._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import cats.effect._
import cats.implicits._
import database.{RedisFrontierStore, FrontierStore}
import dev.profunktor.redis4cats.{RedisCommands, Redis}
import dev.profunktor.redis4cats.effect.Log.Stdout._

import java.util.concurrent.Executors

class Application()(implicit
    ec: ExecutionContext,
    cs: ContextShift[IO],
    timer: Timer[IO]
) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def execute(): IO[ExitCode] = {
    val config = loadConfig
    //TODO: Load concurrency param from config

    withRedisHandle(newExecutionContext(10), config.frontierConfig) {
      redisHandle =>
        val frontierStore: FrontierStore =
          RedisFrontierStore(config.frontierConfig, redisHandle)
        withBlazeClient(newExecutionContext(50)) { client =>
          for {
            stream <-
              ParallelScrapingStream(config.streamConfig, client, frontierStore)
                .runForever()
          } yield stream
        }
    }
  }

  private def newExecutionContext(
      threads: Int
  ): ExecutionContextExecutor =
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(threads)
    )


  private def withBlazeClient(
      executionContext: ExecutionContext
  )(f: Client[IO] => IO[ExitCode]): IO[ExitCode] =
    BlazeClientBuilder[IO](executionContext).resource.use(f(_))

  private def withRedisHandle(
      executionContext: ExecutionContext, //TODO: Use new EC for Redis commands
      frontierConfig: FrontierConfig
  )(f: RedisCommands[IO, String, String] => IO[ExitCode]): IO[ExitCode] =
    Redis[IO].utf8(frontierConfig.redisConnectionString).use(f(_))

  private def loadConfig: Config =
    ConfigSource.default.loadOrThrow[Config]
}
