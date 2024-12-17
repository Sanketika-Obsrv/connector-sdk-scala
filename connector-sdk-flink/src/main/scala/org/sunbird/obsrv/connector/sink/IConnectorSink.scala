package org.sunbird.obsrv.connector.sink

import com.typesafe.config.Config
import org.apache.flink.streaming.api.datastream.{DataStream, SingleOutputStreamOperator, WindowedStream}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.windows.Window
import org.sunbird.obsrv.connector.model.Models.ConnectorContext
import org.sunbird.obsrv.job.exception.UnsupportedDataFormatException

trait IConnectorSink extends Serializable {

  @throws[UnsupportedDataFormatException]
  def setSinkStream(env: StreamExecutionEnvironment, config: Config, dataStream: SingleOutputStreamOperator[String]): Unit

  def getSinkFunction(connectorCtx: ConnectorContext, config: Config): SinkConnectorFunction
}

trait IConnectorWindowSink[W <: Window] {

  @throws[UnsupportedDataFormatException]
  def getWindowedStream(dataStream: DataStream[String], config: Config): WindowedStream[String, String, W]

  @throws[UnsupportedDataFormatException]
  def setSinkStream(env: StreamExecutionEnvironment, config: Config, dataStream: SingleOutputStreamOperator[String]): Unit

  def getSinkFunction(connectorCtx: ConnectorContext, config: Config): SinkConnectorWindowFunction[W]

}