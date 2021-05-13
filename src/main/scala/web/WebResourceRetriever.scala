package web

import cats.effect.IO

trait WebResourceRetriever {
  def retrieveResource(URL: String, timeoutMS: Int): IO[Unit]
}
