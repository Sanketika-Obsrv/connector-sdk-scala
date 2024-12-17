package org.sunbird.obsrv.connector

import com.typesafe.config.ConfigFactory
import org.sunbird.obsrv.connector.sink.{IConnectorSink, SinkConnector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseFlinkSinkConnectorSpec extends BaseFlinkConnectorSpec {

  def getConnector(): IConnectorSink

  def validateConnector(): Unit

  "BaseFlinkSinkConnectorSpec" should s"test the ${getConnectorName()} connector" in {
    val connConfig = ConfigFactory.load(getConnectorConfigFile())
    Future {
      SinkConnector.process(Array("--config.file.path", getConnectorConfigFile()), getConnector())(new EventSource())
    }
    Thread.sleep(10000)
    validateConnector()
    val metrics = getMetrics(metricsReporter, connConfig.getString("metadata.id"))
    validateMetrics(metrics)
  }
}