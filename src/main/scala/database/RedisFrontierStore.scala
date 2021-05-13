package database
import java.util.UUID
import cats.effect.IO
import cats.data.{OptionT, EitherT}
import cats.implicits.catsSyntaxFlatMapOps
import config.FrontierConfig
import dev.profunktor.redis4cats.RedisCommands
import domain.FrontierTarget
import io.circe.parser._
import org.slf4j.{Logger, LoggerFactory}

class RedisFrontierStore(
    frontierConfig: FrontierConfig,
    redisHandle: RedisCommands[IO, String, String]
) extends FrontierStore {
  private val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  override def getFrontierByJobID(id: UUID): IO[Set[ResourceURI]] =
    redisHandle.sMembers(id.toString)

  override def popRandomFromFrontierForJobID(
      id: UUID
  ): OptionT[IO, FrontierTarget] = {
    val retrieval: IO[Option[String]] = redisHandle.sPop(id.toString)

    val result: IO[Option[FrontierTarget]] = retrieval.map {
      case Some(target) => decode[FrontierTarget](target).toOption
      case None => {
        log.error(s"Failed to pop URI from frontier for job ${id.toString}")
        None
      } 
    }

    OptionT(result)
  }

  override def pushToFrontierForJobID(id: UUID, value: String): IO[Unit] =
    redisHandle.sAdd(id.toString, value).map(_ => ())

  override def removeFrontierForJobID(id: UUID): IO[Unit] =
    redisHandle.sRem(id.toString)
}

object RedisFrontierStore {
  def apply(
      frontierConfig: FrontierConfig,
      redisHandle: RedisCommands[IO, String, String]
  ): RedisFrontierStore = {
    new RedisFrontierStore(frontierConfig, redisHandle)
  }
}
