## Problem analysis

Redis itself is a in-memory cache, potential reasons for putting another in-mem cache in-front of it:

* To reduce the load on the redis server  
* To reduce the latency for fetching data if the server has a limited bandwidth or if it's geographically distant from clients

For the second use case, caching larger objects would give more visible results than caching small objects

Important constraints:
* A key size of a record in redis is limited by 512 MiB
* A record size in redis is limited by 512 MiB
* HTTP URL length is not limited by RFC, but in practice URLs longer than 2K can experience issues with processing software  

Given that keys & values can potentially be relatively large, it's important to limit the cache size by memory usage in addition to keys set size.


## Cache design 

## Redis client

## Monitoring & Alerts


## Implementation plan

1. Redis-client
   - alternative client
2. Cache              
   - skip large objects
3. Memory control     [Done]
4. Webserver          [Done]
5. Monitoring         [Done]
6. Docker             [Done]
7. Integration tests  [90%]

Challenges:

max value 512MB
max key 512MB

http get 2,048 characters


- timeouts

Memory pressure
  - size > threshold
  - if mem limit hit strategy
  

- cache nil ?

- embedded redis for tests

- check if key being retrieved for concurrent user


