Vamp Pulse
===============================

Pulse is a metric aggregation engine which allows you to store and aggregate events.   


What it does?

- Consumes streams of events either from kafka or SSE (server sent events), it is designed to be used with vamp-loadbalancer
    and vamp-core, but you can actually integrate it with your own systems as long as the metric format in the stream
    corresponds to `{"tags":["test_fe_1.frontend.scur"],"value":0,"timestamp":"2015-02-18T04:57:56+00:00"}`
    for SSE event name is simply "metric"
- Stores these events into ElasticSearch
- Enables the possibility to send your events through rest api
- Allows aggregation and retrieval of metrics/events


How to use?

- There is still no artifact available in any of the maven repositories, so you would need:
    1. JDK 8 and sbt installed
    2. Check out our vamp-common repository and install it to your local maven repo, README for vamp-common could be found
    in the corresponding repo

- Here is a step by step guide to run this tool via sbt:
    1. Configuration  `src/main/resources/application.conf`
        a. First, a stream has to be configured
          - SSE

            ```stream = {
               driver = "sse"
               url = "http://10.184.88.251:10001/v1/stats/stream"
              }```

          - Kafka

            ```  stream = {
                 driver = "kafka"
                 url = "localhost:2181"
                 topic = "metric"
                 group = "vamp-pulse"
                 partitions = "1"
                 } ```

