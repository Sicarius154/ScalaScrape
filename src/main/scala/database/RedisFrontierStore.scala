package database
import java.util.UUID
import cats.effect.IO
import cats.data.EitherT
import config.FrontierConfig
import dev.profunktor.redis4cats.RedisCommands
import domain.FrontierTarget

class RedisFrontierStore(frontierConfig: FrontierConfig, redisHandle: RedisCommands[IO, String, String]) extends FrontierStore {
  override def getFrontierByJobID(id: UUID): IO[Set[ResourceURI]] = redisHandle.sMembers(id.toString)

  override def popFromFrontierForJobID(id: UUID): EitherT[IO, String, FrontierTarget] = ???

  override def pushToFrontierForJobID(id: UUID, value: String): IO[Unit] =
    redisHandle.sAdd(id.toString, value).map(_ => ())


  override def removeFrontierForJobID(id: UUID): IO[Unit] = ???
}

object RedisFrontierStore {
  def apply(frontierConfig: FrontierConfig, redisHandle: RedisCommands[IO, String, String]): RedisFrontierStore = {
      new RedisFrontierStore(frontierConfig, redisHandle)
  }
}