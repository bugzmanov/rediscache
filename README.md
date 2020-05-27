## Requirements

To build a transparent Redis proxy service. This proxy is implemented as an HTTP web service which allows the ability to add additional features on top of Redis (e.g. caching and sharding)


## Problem analysis

Redis itself is an in-memory cache, potential reasons for putting another in-mem cache in-front of it:

* To reduce the load on the Redis server  
* To reduce the latency for fetching data if the server has limited bandwidth or if it's geographically distant from clients

For the second use case, caching larger objects would give more visible results

Important constraints:
* A key size of a record in Redis is limited at 512 MiB
* A record size in Redis is limited at 512 MiB
* HTTP URL length is not limited by RFC, but in practice URLs longer than 2K can experience issues with processing software (like firewalls and proxies)  

Given that keys & values can potentially be relatively large, it's important to keep control of memory usage in addition to keys set size.

## Protocol specification

`GET /v1/<key>`

Response content type: `application/octet-stream`

| Status Code   | Description           | 
| ------------- |:-------------:| 
| 200           | Successfull operation | 
| 404           | No data for that key      | 
| 500           | Service error      | 


## Running instructions

```bash

git clone https://github.com/bugzmanov/rediscache.git
cd rediscache 
make test

```

Then you can open grafana dashboard: `http://<host>:3000/d/dw2aBiqkz/mydashboard?refresh=5s&orgId=1` (admin/admin)

To run the longer demo:

```
    make demo
```

Configuration file for the docker-compose environment:
https://github.com/bugzmanov/rediscache/blob/master/config/application-docker.conf

## Overall description

* Runtime & Language: scala on JVM 
* Webserver: akka-http (https://doc.akka.io/docs/akka-http/current/index.html)
* Caching library: guava cache (https://github.com/google/guava/wiki/CachesExplained)
* Redis client: jedis (https://github.com/xetorthio/jedis)
* Monitoring: micrometer (https://micrometer.io/), graphite (https://graphiteapp.org/), grafana (https://grafana.com/grafana/)
* Performance tests: locust (https://locust.io/)

## Cache design 

Guava caching library provides support for main requirements: 
- eviction after TTL
- LRU eviction after hitting a predefined capacity limit

Webcache is not caching nil results from Redis, as those requests are inexpensive.

Guava doesn't support weight and size limits simultaneously.
To avoid running out of memory situation, the cache is configured to wrap values in soft-references
(https://en.wikipedia.org/wiki/Soft_reference).

This provides more stable behavior in case of memory overcommitment at the expense of less predictable latency.

Downsides of soft-references:
* stop-the-world GC pause become noticeably longer
* Have to keep track of object size - because they might disappear from the cache (https://github.com/bugzmanov/rediscache/blob/master/src/main/scala/com/bugzmanov/cache/Cache.scala#L20)


Alternatives to explore: 
* caffeine - https://github.com/ben-manes/caffeine
Similar API, better performance, worse memory footprint

* ehcache - https://www.ehcache.org/
The main difference: it provides API for off-heap based cache. 
Can be used to create multi-layer cache: on-heap for small objects, off-heap for large objects


## Redis client

Almost all clients of Redis for JVM languages suffer from the same downside - they fully accumulate response from Redis in a buffer
before returning it to the requesting side. 
Given that a record in Redis can be up to 512 MiB this puts significant constraints on how many clients can the  webcache serve 
simultaneously.

The approach I've implemented so far:
* a configuration parameter on how many active connections can web cache handle. This puts the burden of making webcache stable on
DevOps. A user of the webcache has a better understanding of how data size distribution looks like and can decide on how
many concurrent clients webcache should handle.

An alternative approach:
* provide configuration parameter allowing webcache to avoid caching objects larger specified size. For those objects webcache can serve
as a proxy between a client and Redis instance, using relatively small buffers.

Redis protocol specifies the size of the payload in RESP Bulk Strings. This can be used to make an early decision to cache or proxy.
I didn't have enough time to play around with this approach. It doesn't look hard.

This approach makes sense only if large objects population is small and is accessed infrequently.

## Monitoring & Alerting

Webcache generates metrics for all layers: web, cache, Redis client.
Metrics are generated using "micrometer" library, and can be adapted to report to any metrics collection system.

Currently metrics are reported to graphite and can be observed using the grafana dashboard.

![image](https://user-images.githubusercontent.com/502482/79080375-517fbb00-7ce2-11ea-8fa8-b0262a141e21.png)


### Alerting conditions:

* Running out of memory: when free JVM memory size becomes less than 300 MiB. Indicate service overload. 
<br/>Mitigation strategy: 
    - reduce cache capacity and/or decrease the number of allowed active connections.

* Cache is down: graphite hasn't received any heartbeats from the webcache for the last minute. 
<br/>Mitigation strategy: 
    - make sure cache is up and running
    - make sure it can connect to graphite instance

* Redis access errors: webcache is getting errors while reading data from Redis.
<br/>Mitigation strategy:
    - make sure backing Redis instance is up and running
    - make sure webcache can connect to the Redis instance

## Load testing

https://github.com/bugzmanov/rediscache/blob/master/locust/locustfile.py

## Implementation plan

1. Redis-client       [90%]
   - opt: a custom client that provides access to underlying InputStream
2. Cache              [90%]
   - opt: skip large objects
   - opt: ehcache with off-heap
3. Memory control     [Done]
4. Webserver          [Done]
5. Monitoring         [Done]
6. Docker             [Done]
7. Tests              [90%]
