import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import io.circe.{Encoder, Decoder}

package object domain {
    implicit val politenessPolicyDecoder: Decoder[PolitenessPolicy] =
    deriveDecoder

  implicit val politenessPolicyEncoder: Encoder[PolitenessPolicy] =
    deriveEncoder

  implicit val scrapeTargetDecoder: Decoder[TargetSeed] =
    deriveDecoder

  implicit val scrapeTargetEncoder: Encoder[TargetSeed] =
    deriveEncoder

  implicit val frontierTargetDecoder: Decoder[FrontierTarget] =
    deriveDecoder

  implicit val frontierTargetEncoder: Encoder[FrontierTarget] =
    deriveEncoder
}
