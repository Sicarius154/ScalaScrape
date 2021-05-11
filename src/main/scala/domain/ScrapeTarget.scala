package domain

case class ScrapeTarget (seedURL: String, politenessPolicy: PolitenessPolicy, excludedURLs: Seq[String])
