# Vamp Pulse
[![Travis build badge](https://travis-ci.org/magneticio/vamp-pulse.svg?branch=master)](https://travis-ci.org/magneticio/vamp-pulse)

Vamp Pulse is an aggregation engine which allows you to store and aggregate events in Elasticsearch using
either REST calls, SSE or Kafka feeds. It works together with [Vamp Router](https://github.com/magneticio/vamp-router) and [Vamp Core](https://github.com/magneticio/vamp-core)

## building and running

Clone this repo and use SBT to run it. You specificy your specific config file based on [reference.conf](https://github.com/magneticio/vamp-pulse/tree/master/server/src/main/resources/reference.conf) and optionally your logback.xml file:
```bash
sbt -Dconfig.file=conf/application.conf -Dlogback.configurationFile=conf/logback.xml run
```

For more documentation on installing and using Vamp Pulse check [http://vamp.io](http://vamp.io/documentation/installation/configuration/)