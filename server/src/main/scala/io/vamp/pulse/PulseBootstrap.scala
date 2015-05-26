package io.vamp.pulse

object PulseBootstrap extends App {
  //  private implicit val system = ActorSystem("pulse-system")
  //  private implicit val mat = ActorFlowMaterializer()
  //  private implicit val executionContext = system.dispatcher
  //
  //  private val config = ConfigFactory.load()
  //
  //  private val streamDriverType = Try(config.getString("stream.driver")).getOrElse("sse")
  //
  //
  //  private def startup(): Unit = {
  //
  //    val eventDao = new ElasticSearchEventDAO
  //
  //    eventDao.createIndex
  //
  //    val (metricManagerSource: PropsSource[Event], driver: Driver) = initSourceAndDriver
  //
  //    val materializedMap = metricManagerSource.groupedWithin(1000, 1 millis)
  //      .map { eventList => eventDao.batchInsert(eventList); eventList }
  //      .to(Sink.ignore).run()
  //
  //    driver.start(materializedMap.get(metricManagerSource), system)
  //
  //    // start HTTP server
  //  }
  //
  //  private def initSourceAndDriver = streamDriverType match {
  //    case "sse" =>
  //      (Source[Event](SSEMetricsPublisher.props), SseDriver)
  //
  //    case "kafka" =>
  //      (Source[Event](KafkaMetricsPublisher.props), KafkaDriver)
  //
  //    case _ => throw new Exception(s"Driver $streamDriverType not found")
  //  }
}
