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
 First, a stream has to be configured

```
SSE
stream = {
    driver = "sse"
    url = "http://10.184.88.251:10001/v1/stats/stream"
}

Kafka
    stream = {
        driver = "kafka"
            url = "localhost:2181"
            topic = "metric"
            group = "vamp-pulse"
            partitions = "1"
    }
```

Storage configuration
               
        storage {
                es {
                  port = 9200
                  host = "localhost"

                  embedded {
                    enabled = true // this basically starts an elastic search server on localhost:9300
                    http = true // this also opens ES rest api to test results, localhost:9200
                  }
            
                  cluster {
                    name = "elasticsearch"
                  }

             }
        }

Configure rest api 
        
        http {
          interface = 0.0.0.0
          port = 8083
          response.timeout = 5
        }
        
    
That's basically it, now you can type `sbt run` in the project root folder and start using the API. Rest API endpoints and their decription could be found in the project wiki.
        
