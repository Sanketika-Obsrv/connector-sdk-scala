package org.sunbird.obsrv.connector.sink

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.datastream.WindowedStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.slf4j.LoggerFactory
import org.sunbird.obsrv.connector.model.Models.{ConnectorContext, ConnectorInstance}
import org.sunbird.obsrv.connector.service.ConnectorRegistry
import org.sunbird.obsrv.connector.util.EncryptionUtil
import org.sunbird.obsrv.job.exception.ObsrvException
import org.sunbird.obsrv.job.util._

import java.io.File

object SinkConnector {

  private[this] val logger = LoggerFactory.getLogger(SinkConnector.getClass)

  private def getConfig(args: Array[String]): Config = {
    val configFilePathOpt = Option(ParameterTool.fromArgs(args).get("config.file.path"))
    val configFilePath = configFilePathOpt.getOrElse("config.json")
    val configFile = new File(configFilePath)
    val config: Config = if (configFile.exists()) {
      logger.info("Loading configuration file from path: " + configFilePath + "...")
      ConfigFactory.parseFile(configFile).resolve()
    } else {
      logger.info("Loading configuration file connector.conf inside the jar...")
      ConfigFactory.load(configFilePath).withFallback(ConfigFactory.load("connector.conf")).withFallback(ConfigFactory.systemEnvironment())
    }
    config
  }

  def process(args: Array[String], connector: IConnectorSink)(implicit source: SourceFunction[String] = null): Unit = {
    val config = getConfig(args)
    logger.info("config in use: " + config)
    val connectorId = Option(ParameterTool.fromArgs(args).get("metadata.id")).getOrElse(config.getString("metadata.id"))
    implicit val pgConfig: PostgresConnectionConfig = DatasetRegistryConfig.getPostgresConfig(ParameterTool.fromArgs(args).get("config.file.path"))
    implicit val env: StreamExecutionEnvironment = FlinkUtil.getExecutionContext(args, config)
    implicit val kc: FlinkKafkaConnector = if (source == null) new FlinkKafkaConnector(config) else null
    implicit val encUtil: EncryptionUtil = new EncryptionUtil(config.getString("obsrv.encryption.key"))
    val connectorInstancesMap = getConnectorInstances(args, config)
    connectorInstancesMap.foreach(entry => {
      val connectorConfig = getConnectorConfig(entry, config)
      try {
        processConnectorInstance(connector, entry.connectorContext, connectorConfig)(env, kc, source)
      } catch {
        case ex: ObsrvException =>
          logger.error(s"Unable to process connector instance | ${JSONUtil.serialize(entry)} | error: ${JSONUtil.serialize(ex.error)}", ex)
        // TODO: How to raise an event for alerts?
      }
    })
    env.execute(connectorId)
  }

  def processWindow[W <: Window](args: Array[String], connector: IConnectorWindowSink[W])
                                (implicit source: SourceFunction[String]): Unit = {
    val config = getConfig(args)
    val connectorId = Option(ParameterTool.fromArgs(args).get("metadata.id")).getOrElse(config.getString("metadata.id"))
    implicit val pgConfig: PostgresConnectionConfig = DatasetRegistryConfig.getPostgresConfig(ParameterTool.fromArgs(args).get("config.file.path"))
    implicit val env: StreamExecutionEnvironment = FlinkUtil.getExecutionContext(args, config)
    implicit val kc: FlinkKafkaConnector = if (source == null) new FlinkKafkaConnector(config) else null
    implicit val encUtil: EncryptionUtil = new EncryptionUtil(config.getString("obsrv.encryption.key"))
    val connectorInstancesMap = getConnectorInstances(args, config)
    connectorInstancesMap.foreach(entry => {
      val connectorConfig = getConnectorConfig(entry, config)
      try {
        processConnectorInstanceWindow(connector, entry.connectorContext, connectorConfig)(env, kc, source)
      } catch {
        case ex: ObsrvException =>
          logger.error(s"Unable to process connector instance |  ${JSONUtil.serialize(entry)} | error: ${JSONUtil.serialize(ex.error)}", ex)
        // TODO: How to raise an event for alerts?
      }
    })
    env.execute(connectorId)
  }

  private def processConnectorInstanceWindow[W <: Window](connector: IConnectorWindowSink[W], cc: ConnectorContext, config: Config)
                                                         (implicit env: StreamExecutionEnvironment, kafkaConnector: FlinkKafkaConnector, source: SourceFunction[String]): Unit = {

    logger.info("[Start] Register connector instance streams")
    val sourceStream = if (source == null) {
      env.fromSource(
        kafkaConnector.kafkaStringSource(cc.datasetTopic.get, cc.connectorInstanceId),
        WatermarkStrategy.noWatermarks[String](), config.getString("source_kafka_consumer_id")
      ).uid(cc.connectorInstanceId).setParallelism(config.getInt("task.consumer.parallelism")).rebalance()
    } else {
      env.addSource(source).uid(cc.connectorInstanceId).setParallelism(config.getInt("task.consumer.parallelism")).rebalance()
    }
    val windowStream: WindowedStream[String, String, W] = connector.getWindowedStream(sourceStream, config)
    val dataStream = windowStream.process(connector.getSinkFunction(cc, config))
    connector.setSinkStream(env, config, dataStream)
    logger.info("[End] Register connector instance streams")
  }

  private def processConnectorInstance(connector: IConnectorSink, cc: ConnectorContext, config: Config)
                                      (implicit env: StreamExecutionEnvironment, kafkaConnector: FlinkKafkaConnector, source: SourceFunction[String]): Unit = {

    logger.info("[Start] Register connector instance streams")
    val sourceStream = if (source == null) {
      env.fromSource(
        kafkaConnector.kafkaStringSource(cc.datasetTopic.get, cc.connectorInstanceId),
        WatermarkStrategy.noWatermarks[String](), config.getString("source_kafka_consumer_id")
      ).uid(cc.connectorInstanceId).setParallelism(config.getInt("task.consumer.parallelism")).rebalance()
    } else {
      env.addSource(source).uid(cc.connectorInstanceId).setParallelism(config.getInt("task.consumer.parallelism")).rebalance()
    }

    val dataStream = sourceStream.process(connector.getSinkFunction(cc, config))
    connector.setSinkStream(env, config, dataStream)
    logger.info("[End] Register connector instance streams")
  }

  private def getConnectorConfig(connectorInstance: ConnectorInstance, config: Config)(implicit encryptionUtil: EncryptionUtil): Config = {
    ConfigFactory.parseString(encryptionUtil.decrypt(connectorInstance.connectorConfig))
      .withFallback(ConfigFactory.parseString(connectorInstance.operationsConfig))
      .withFallback(config)
  }

  private def getConnectorInstances(args: Array[String], config: Config)
                                   (implicit postgresConnectionConfig: PostgresConnectionConfig): List[ConnectorInstance] = {
    val connectorId = Option(ParameterTool.fromArgs(args).get("metadata.id")).getOrElse(config.getString("metadata.id"))
    val connectorInstances = ConnectorRegistry.getConnectorInstances(connectorId)
    connectorInstances.map(instances => instances).orElse(Some(List[ConnectorInstance]())).get
  }
}


