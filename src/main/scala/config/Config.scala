package config

case class Config(
  streamConfig: StreamConfig,
  frontierConfig: FrontierConfig
)

case class StreamConfig(bootstrapServer: String, seedTopic: String, consumerGroup: String)

case class FrontierConfig(redisConnectionString: String)