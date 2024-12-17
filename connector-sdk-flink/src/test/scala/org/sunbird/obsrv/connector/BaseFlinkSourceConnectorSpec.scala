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

  "BaseFlinkConnectorSpec" should s"test the ${getConnectorName()} connector" in {
    val connConfig = ConfigFactory.load(getConnectorConfigFile())
    EventsSink.failedEvents.clear()
    EventsSink.successEvents.clear()
    Future {
      SourceConnector.process(Array("--config.file.path", getConnectorConfigFile()), getConnector())(new SuccessSink(), new FailedSink())
    }
    Thread.sleep(10000)
    val successEvents = EventsSink.successEvents.asScala.map(f => {
      val event = JSONUtil.deserialize[Map[String, AnyRef]](f).get("event").get
      JSONUtil.serialize(event)
    }).asJava
    testSuccessEvents(successEvents)
    testFailedEvents(EventsSink.failedEvents)
    validateMetrics(getMetrics(metricsReporter, connConfig.getString("metadata.id")))
  }
}