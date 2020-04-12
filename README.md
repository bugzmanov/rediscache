# Plan

1. Redis-client
2. Cache              [Done]
3. Memory control
4. Webserver          [Done]
5. Monitoring         [Done]
6. Docker             [Done]
7. Integration tests  [50%]

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


