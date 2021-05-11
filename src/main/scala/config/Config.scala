package config

case class Config(
  streamConfig: StreamConfig
)

case class StreamConfig(bootstrapServer: String, seedTopic: String, consumerGroup: String)

