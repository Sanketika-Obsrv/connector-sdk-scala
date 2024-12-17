package org.sunbird.obsrv.connector

import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.Matchers.{be, convertToAnyShouldWrapper}
import org.sunbird.obsrv.connector.sink.IConnectorSink

import scala.concurrent.duration._

class SampleSinkConnectorTestSpec extends BaseFlinkSinkConnectorSpec with Serializable {

  val customKafkaConsumerProperties: Map[String, String] = Map[String, String]("auto.offset.reset" -> "earliest", "group.id" -> "test-connector-group")
  implicit val deserializer: StringDeserializer = new StringDeserializer()
  implicit val embeddedKafkaConfig: EmbeddedKafkaConfig =
    EmbeddedKafkaConfig(
      kafkaPort = 9093,
      zooKeeperPort = 2183,
      customConsumerProperties = customKafkaConsumerProperties
    )

  override def beforeAll(): Unit = {
    EmbeddedKafka.start()(embeddedKafkaConfig)
    createTestTopics()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    EmbeddedKafka.stop()
  }

  def createTestTopics(): Unit = {
    EmbeddedKafka.createCustomTopic(topic = "vendor1-topic")
    EmbeddedKafka.createCustomTopic(topic = "vendor2-topic")
  }

  override def getConnectorName(): String = "SampleSinkConnector"

  override def getConnector(): IConnectorSink = new SampleConnectorSink()

  override def getConnectorConfigFile(): String = "test-sink-config.json"

  override def getConnectorConfig(): Map[String, AnyRef] = {
    Map(
      "kafka_broker_servers" -> "localhost:9093"
    )
  }

  override def validateMetrics(metrics: Map[String, Long]): Unit = {
    metrics("vendor1_count") should be(2)
    metrics("vendor2_count") should be(1)
  }

  override def validateConnector(): Unit = {
    val vendor1Events = EmbeddedKafka.consumeNumberMessagesFrom[String]("vendor1-topic", 2, timeout = 10.seconds)
    val vendor2Events = EmbeddedKafka.consumeNumberMessagesFrom[String]("vendor2-topic", 1, timeout = 10.seconds)
    vendor1Events.size should be(2)
    vendor2Events.size should be(1)
  }
}