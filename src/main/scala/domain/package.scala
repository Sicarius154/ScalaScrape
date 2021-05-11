import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import io.circe.{Encoder, Decoder}

package object domain {
    implicit val politenessPolicyDecoder: Decoder[PolitenessPolicy] =
    deriveDecoder

  implicit val politenessPolicyEncoder: Encoder[PolitenessPolicy] =
    deriveEncoder

  implicit val scrapeTargetDecoder: Decoder[ScrapeTarget] =
    deriveDecoder

  implicit val scrapeTargetEncoder: Encoder[ScrapeTarget] =
    deriveEncoder

}
