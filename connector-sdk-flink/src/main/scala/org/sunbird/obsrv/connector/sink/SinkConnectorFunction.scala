package org.sunbird.obsrv.connector.sink

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.sunbird.obsrv.connector.model.Models.ConnectorContext
import org.sunbird.obsrv.job.function.BaseProcessFunction
import org.sunbird.obsrv.job.util.Metrics

abstract class SinkConnectorFunction(connectorCtx: ConnectorContext) extends BaseProcessFunction[String, String] {

  private def incMetric(metric: String, count: Long)(implicit metrics: Metrics) : Unit = {
    if(getMetrics().contains(metric)) {
      metrics.incCounter(connectorCtx.connectorInstanceId, metric, count)
    }
  }

  override def processElement(event: String, context: ProcessFunction[String, String]#Context, metrics: Metrics): Unit = {
    super.initMetrics(connectorCtx.connectorId, connectorCtx.connectorInstanceId)
    implicit val mtx: Metrics = metrics
    processEvent(event, incMetric, context)
  }

  def processEvent(event: String, incMetric: (String, Long) => Unit, ctx: ProcessFunction[String, String]#Context): Unit

}
