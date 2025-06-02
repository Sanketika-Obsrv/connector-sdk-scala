package org.sunbird.obsrv.connector

import com.typesafe.config.ConfigFactory
import org.sunbird.obsrv.connector.source.{IConnectorSource, SourceConnector}
import org.sunbird.obsrv.job.util.JSONUtil

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseFlinkSourceConnectorSpec extends BaseFlinkConnectorSpec {

  def getConnector(): IConnectorSource

  def testFailedEvents(events: java.util.List[String]): Unit

  def testSuccessEvents(events: java.util.List[String]): Unit

  "BaseFlinkSourceConnectorSpec" should s"test the ${getConnectorName()} connector with metadata.id" in {
    val connConfig = ConfigFactory.load(getConnectorConfigFile())
    val metadataId = connConfig.getString("metadata.id")
    EventsSink.failedEvents.clear()
    EventsSink.successEvents.clear()
    Future {
      SourceConnector.process(Array("--config.file.path", getConnectorConfigFile(), "--metadata.id", metadataId), getConnector())(new SuccessSink(), new FailedSink())
    }
    Thread.sleep(10000)
    val successEvents = EventsSink.successEvents.asScala.map(f => {
      val event = JSONUtil.deserialize[Map[String, AnyRef]](f).get("event").get
      JSONUtil.serialize(event)
    }).asJava
    testSuccessEvents(successEvents)
    testFailedEvents(EventsSink.failedEvents)
    validateMetrics(getMetrics(metricsReporter, metadataId))
  }

  if (getConnectorName().equals("SampleSourceConnector")) {
    "BaseFlinkSourceConnectorSpec" should s"test the ${getConnectorName()} connector with connector.instance.id" in {
      val instanceId = "c1"
      EventsSink.failedEvents.clear()
      EventsSink.successEvents.clear()
      Future {
        SourceConnector.process(Array("--config.file.path", getConnectorConfigFile(), "--connector.instance.id", instanceId), getConnector())(new SuccessSink(), new FailedSink())
      }
      Thread.sleep(10000)
      val successEvents = EventsSink.successEvents.asScala.map(f => {
        val event = JSONUtil.deserialize[Map[String, AnyRef]](f).get("event").get
        JSONUtil.serialize(event)
      }).asJava
      testSuccessEvents(successEvents)
      testFailedEvents(EventsSink.failedEvents)
      validateMetrics(getMetrics(metricsReporter, instanceId))
    }
  }
}