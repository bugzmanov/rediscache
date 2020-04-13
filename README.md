## Problem analysis

Redis itself is a in-memory cache, potential reasons for putting another in-mem cache in-front of it:

* To reduce the load on the redis server  
* To reduce the latency for fetching data if the server has a limited bandwidth or if it's geographically distant from clients

For the second use case, caching larger objects would give more visible results

Important constraints:
* A key size of a record in redis is limited at 512 MiB
* A record size in redis is limited at 512 MiB
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

To run longer demo:

```
    make demo
```

Configuration file for docker-compose environment:
https://github.com/bugzmanov/rediscache/blob/master/config/application-docker.conf

## Overall description

* Runtime & Language: scala on JVM 
* Webserver: akka-http (https://doc.akka.io/docs/akka-http/current/index.html)
* Caching library: guava cache (https://github.com/google/guava/wiki/CachesExplained)
* Redis client: jedis (https://github.com/xetorthio/jedis)
* Monitoring: micrometer (https://micrometer.io/), graphite (https://graphiteapp.org/), grafana (https://grafana.com/grafana/)
* Performance tests: locust (https://locust.io/)

## Cache design 

Guava caching library provide support for main requirements: 
- eviction after ttl
- LRU eviction after hitting predefined capacity limit

Webcache is not caching nil results from redis, as those requests are inexpensive.

Guava doesn't support weight and size limits simultaneously.
In order to avoid running out of memory situation, cache is configures to wrap values in soft-references
(https://en.wikipedia.org/wiki/Soft_reference).

This provide approach provide more stable behaviour in case of memory overcommitment in expense of less predictable latency.

Downsides of soft-references:
* Stop-the-world GC pause become noticeably longer
* Have to keep track of object size - because they might disappear from the cache (https://github.com/bugzmanov/rediscache/blob/master/src/main/scala/com/bugzmanov/cache/Cache.scala#L20)


Alternatives to explore: 
* caffeine - https://github.com/ben-manes/caffeine
Similar API, better performance, worse memory footprint

* ehcache - https://www.ehcache.org/
Main difference: provide api for off-heap based cache. 
Can be used to create multi-layer cache: on-heap for small objects, off-heap for large objects


## Redis client

Almost all clients of redis for jvm languages suffer from the same downside - they fully accumulate response from redis in a buffer
before returning it to the requesting side. 
Given that a record in redis can be up to 512 MiB this puts significant constraints on how many clients can the  webcache serve 
simultaneously.

The approach i've implemented so far:
* a configuration parameter on how many active connections can web cache handle. This puts the burden of making webcache stable on
devops. A user of the webcache has better understanding of how data size distribution looks like and can make a decision on how
many concurrent clients webcache should handle.

An alternative approach:
* provide configuration parameter allowing webcache to avoid caching objects larger specified size. For those objects webcache can serve
as a proxy between a client and redis instance, using relatively small buffers.

Redis protocol specifies size of payload in RESP Bulk Strings. This can be used to make early decision to cache or to proxy.
I didn't have enough time to play around with this approach. It doesn't look hard.

This approach makes sense only in case of smaller population of large objects that are accessed infrequently.

## Monitoring & Alerting

Webcache generates metrics for all layers: web, cache, redisclient.
Metrics are generated using micrometer library, and can be adapted to report to any metrics collection system.

Currently metrics are reported to graphite and can be observed using grafana dashboard.

![image](https://user-images.githubusercontent.com/502482/79080375-517fbb00-7ce2-11ea-8fa8-b0262a141e21.png)


### Alerting conditions:

* Running out of memory: when free jvm memory size becomes less than 300 MiB. Indicate service overload. 
Strategy: 
    - reduce cache capacity and/or decrease number of allowed active connections.

* Cache is down: no heat beats from webcache reached graphite instance for the last minute. 
Strategy: 
    - make sure cache is up and running
    - make sure it can connect to graphite instance

* Redis access errors: webcache is getting errors while reading data from redis.
Stategy:
    - make sure backing redis instance is up and running
    - make sure webcache can connect to to redis instance

## Load testing

https://github.com/bugzmanov/rediscache/blob/master/locust/locustfile.py

## Implementation plan

1. Redis-client       [90%]
   - opt: custom client that provides access to underlying InputStream
2. Cache              [90%]
   - opt: skip large objects
   - opt: ehcache with offheap
3. Memory control     [Done]
4. Webserver          [Done]
5. Monitoring         [Done]
6. Docker             [Done]
7. Tests              [90%]
