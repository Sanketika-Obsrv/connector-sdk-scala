package org.sunbird.obsrv.connector

import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.Matchers._
import org.sunbird.obsrv.connector.source.IConnectorSource

import scala.collection.JavaConverters._

class SampleSourceConnectorTestSpec extends BaseFlinkSourceConnectorSpec with Serializable {

  val customKafkaConsumerProperties: Map[String, String] = Map[String, String]("auto.offset.reset" -> "earliest", "group.id" -> "test-connector-group")
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

  override def beforeEach(): Unit = {
    EmbeddedKafka.publishStringMessageToKafka("test-kafka-topic", EventFixture.INVALID_JSON)
    EmbeddedKafka.publishStringMessageToKafka("test-kafka-topic", EventFixture.VALID_JSON)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    EmbeddedKafka.stop()
  }

  def createTestTopics(): Unit = {
    EmbeddedKafka.createCustomTopic(topic = "test-kafka-topic")
  }

  override def getConnectorName(): String = "SampleSourceConnector"

  override def getConnector(): IConnectorSource = new SampleSourceConnectorSource()

  override def testFailedEvents(events: java.util.List[String]): Unit = {
    events.size() should be (1)
    events.asScala.head should be ("""{"error":{"error_code":"JSON_FORMAT_ERR","error_msg":"Not a valid json"},"event":"{\"name\":\"/v1/sys/health\",\"context\":{\"trace_id\":\"7bba9f33312b3dbb8b2c2c62bb7abe2d\"","connector_ctx":{"connector_id":"sample-source-connector","dataset_id":"d1","connector_instance_id":"c1","connector_type":"source","entryTopic":"ingest","state":{},"stats":{},"datasetTopic":"d1-events"}}""")
  }

  override def testSuccessEvents(events: java.util.List[String]): Unit = {
    events.size() should be (1)
  }

  override def getConnectorConfigFile(): String = "test-source-config.json"

  override def getConnectorConfig(): Map[String, AnyRef] = {
    Map(
      "source_kafka_broker_servers" -> "localhost:9093",
      "source_kafka_consumer_id" -> "kafka-connector",
      "source_kafka_auto_offset_reset" -> "earliest",
      "source_data_format" -> "json",
      "source_kafka_topic" -> "test-kafka-topic",
    )
  }

  override def validateMetrics(metrics: Map[String, Long]): Unit = {
    metrics.get("total_obsrv_success_count").get should be (1)
    metrics.get("total_connector_failed_count").get should be (1)
  }
}