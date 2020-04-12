build: check_java
	./bin/sbt compile test

publish: build
	./bin/sbt docker:publishLocal

create: publish
	docker network create app-tier
	docker-compose up --build -d
	docker build --pull ./locust -t rediscache-test

test: create
	docker run -e TARGET_URL=http://cache:8080 -e REDIS_HOST=redis -e LOCUST_OPTS="--clients=100 -r 10 --no-web --run-time=60" --network app-tier rediscache-test:latest

clean:
	docker-compose down
	docker-compose rm
	docker network rm app-tier
	docker image rm rediscache-test

update-dashboards:
	./update-dashboards.sh


check_java:
	./bin/check_java.sh
