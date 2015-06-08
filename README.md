Vamp Pulse
===============================
[![Travis build badge](https://travis-ci.org/magneticio/vamp-pulse.svg?branch=master)](https://travis-ci.org/magneticio/vamp-pulse)
[![Coverage Status](https://coveralls.io/repos/magneticio/vamp-pulse/badge.svg?branch=master)](https://coveralls.io/r/magneticio/vamp-pulse?branch=master)

Pulse is a metric aggregation engine which allows you to store and aggregate events.   


What it does?

- Consumes streams of events either from kafka or SSE (server sent events), it is designed to be used with vamp-router
    and vamp-core, but you can actually integrate it with your own systems as long as the metric format in the stream
    corresponds to `{"tags":["test_fe_1.frontend.scur"],"value":0,"timestamp":"2015-02-18T04:57:56+00:00"}`
    for SSE event name is simply "metric"
- Stores these events into ElasticSearch
- Enables the possibility to send your events through rest api
- Allows aggregation and retrieval of metrics/events


How to use?

1. JDK 8 and sbt installed

Here is a step by step guide to run this tool via sbt:   
Configuration  `src/main/resources/application.conf` could be copied from reference.conf and modified as needed.   
Example:

```
vamp.pulse {

  hi-message = "Hi, I'm Vamp! How are you?"

  rest-api {
    interface = 0.0.0.0
    port = 8083
    host = "localhost"
    response-timeout = 5 # seconds
  }

  elasticsearch {

    type = "embedded" # "remote" or "embedded"

    response-timeout = 5 # seconds

    url = "http://localhost:9200"

    remote {
      tcp-port = 9300
      host = "localhost"
      cluster-name = "elasticsearch"
    }

    embedded {
      tcp-port = 9300
      cluster-name = "vamp"
      data-directory = "target/elasticsearch"
    }

    index {
      name = "vamp"

      time-format = {
        event = "YYYY-MM"
        router = "YYYY-MM-dd"
      }
    }
  }

  event-stream {
    driver = "none" # "sse", "kafka" or "none"

    sse {

      url = "http://10.169.169.138:10001/v1/stats/stream"

      timeout {
        http.connect = 2000
        sse.connection.checkup = 3000
      }
    }

    kafka {
      url = ""
      topic = "metric"
      group = "vamp-pulse"
      partitions = "1"
    }
  }
}
```       
    
That's basically it, now you can type `sbt run` in the project root folder and start using the API. Rest API endpoints and their description could be found in the project wiki.
        
