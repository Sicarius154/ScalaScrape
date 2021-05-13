package web

import org.http4s.Uri.RegName
import org.http4s.{Query, Uri}
import cats.effect.{ContextShift, Async, Resource, Timer, IO, Sync}
import org.http4s.client.Client
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.DurationInt

class HTTPWebResourceRetriever(httpClient: Client[IO])(implicit
    cs: ContextShift[IO],
    timer: Timer[IO]
) extends WebResourceRetriever {
  private val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def retrieveResource(
      URL: String,
      timeoutMS: Int
  ): IO[Unit] =
    httpClient
      .expect[String](HTTPWebResourceRetriever.http4sURI(false, URL, 80))
      .redeemWith(
        ex =>
          IO(log.error(s"Error retrieving $URL. Message: ${ex.getMessage}")),
        _ => IO(log.info(s"Retrieved for $URL"))
      )
      .timeoutTo(
        timeoutMS.millis,
        IO(log.warn(s"Timed out retrieving $URL after $timeoutMS milliseconds"))
      )
}

object HTTPWebResourceRetriever {
  def apply(httpClient: Client[IO])(implicit
      cs: ContextShift[IO],
      timer: Timer[IO]
  ): HTTPWebResourceRetriever = new HTTPWebResourceRetriever(httpClient)

  private[web] def http4sURI(
      https: Boolean,
      target: String,
      connectionPort: Int = 80
  ): Uri = {
    val httpScheme = Option(if (https) Uri.Scheme.https else Uri.Scheme.http)
    val userCredentials = None
    val host = RegName(target)
    val port = Option(connectionPort)

    val resourcePath =
      "" //TODO: Utilize this param in place of having the full resource identifier in the target field

    val query = Query.empty
    val frag = None

    Uri(
      httpScheme,
      Option(Uri.Authority(userCredentials, host, port)),
      resourcePath,
      query,
      frag
    )
  }
}
