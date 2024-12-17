package org.sunbird.obsrv.connector.sink

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.sunbird.obsrv.connector.model.Models.ConnectorContext
import org.sunbird.obsrv.job.function.BaseWindowProcessFunction
import org.sunbird.obsrv.job.util.Metrics

import java.lang
import scala.collection.JavaConverters._

abstract class SinkConnectorWindowFunction[W <: Window](connectorCtx: ConnectorContext) extends BaseWindowProcessFunction[String, String, String, W] {

  private def incMetric(metric: String, count: Long)(implicit mtx: Metrics): Unit = {
    if (getMetrics().contains(metric)) {
      mtx.incCounter(connectorCtx.connectorInstanceId, metric, count)
    }
  }

  override def process(key: String, context: ProcessWindowFunction[String, String, String, W]#Context, elements: lang.Iterable[String], metrics: Metrics): Unit = {

    super.initMetrics(connectorCtx.connectorId, connectorCtx.connectorInstanceId)
    implicit val mtx: Metrics = metrics
    val eventsList = elements.asScala.toList
    processEvents(key, eventsList, incMetric, context)
  }

  def processEvents(key: String, events: List[String], incMetric: (String, Long) => Unit, context: ProcessWindowFunction[String, String, String, W]#Context): Unit

}
