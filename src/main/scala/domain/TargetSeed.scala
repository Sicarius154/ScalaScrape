package domain

case class TargetSeed(seedURL: String, politenessPolicy: PolitenessPolicy, excludedURLs: Seq[String])
