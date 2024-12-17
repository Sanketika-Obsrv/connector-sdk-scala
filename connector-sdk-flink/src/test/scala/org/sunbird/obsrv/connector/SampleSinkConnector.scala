package org.sunbird.obsrv.connector

import com.typesafe.config.Config
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.connector.base.DeliveryGuarantee
import org.apache.flink.connector.kafka.sink.{KafkaRecordSerializationSchema, KafkaSink}
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.sunbird.obsrv.connector.model.Models
import org.sunbird.obsrv.connector.model.Models.ConnectorContext
import org.sunbird.obsrv.connector.sink.{IConnectorSink, SinkConnector, SinkConnectorFunction}
import org.sunbird.obsrv.job.util.JSONUtil

import java.nio.charset.StandardCharsets
import java.util.Properties

object SampleSinkConnector {
  def main(args: Array[String]): Unit = {
    SinkConnector.process(args, new SampleConnectorSink)
  }
}

class SampleConnectorSink extends IConnectorSink {
  override def setSinkStream(env: StreamExecutionEnvironment, config: Config, dataStream: SingleOutputStreamOperator[String]): Unit = {
    dataStream.getSideOutput(OutputTag[String]("vendor1")).sinkTo(kafkaSink("vendor1-topic", config))
    dataStream.getSideOutput(OutputTag[String]("vendor2")).sinkTo(kafkaSink("vendor2-topic", config))
  }

  def kafkaSink(kafkaTopic: String, config: Config): KafkaSink[String] = {
    KafkaSink.builder[String]()
      .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
      .setRecordSerializer(new SerializationSchema(kafkaTopic))
      .setKafkaProducerConfig(kafkaProducerProperties(config))
      .build()
  }

  def kafkaProducerProperties(config: Config): Properties = {
    val properties = new Properties()
    properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("kafka_broker_servers"))
    properties.put(ProducerConfig.LINGER_MS_CONFIG, config.getInt("kafka.producer.linger.ms"))
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.valueOf(config.getInt("kafka.producer.batch.size")))
    properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy")
    properties
  }

  override def getSinkFunction(connectorCtx: Models.ConnectorContext, config: Config): SinkConnectorFunction = new SampleConnectorFunction(connectorCtx, config)
}

class SampleConnectorFunction(connectorCtx: ConnectorContext, config: Config) extends SinkConnectorFunction(connectorCtx) {
  override def processEvent(event: String, incMetric: (String, Long) => Unit, ctx: ProcessFunction[String, String]#Context): Unit = {
    val eventMap = JSONUtil.deserialize[Map[String, AnyRef]](event)
    if(eventMap("VendorID").equals("1")) {
      incMetric("vendor1_count", 1)
      ctx.output(OutputTag[String]("vendor1"), event)
    } else {
      incMetric("vendor2_count", 1)
      ctx.output(OutputTag[String]("vendor2"), event)
    }
  }

  override def getMetrics(): List[String] = {
    List[String]("vendor1_count", "vendor2_count")
  }
}

class SerializationSchema[T](topic: String) extends KafkaRecordSerializationSchema[T] {

  private val serialVersionUID = -4284080856874185929L

  override def serialize(element: T, context: KafkaRecordSerializationSchema.KafkaSinkContext, timestamp: java.lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    val out = JSONUtil.serialize(element)
    new ProducerRecord[Array[Byte], Array[Byte]](topic, out.getBytes(StandardCharsets.UTF_8))
  }
}