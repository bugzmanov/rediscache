build:
	source ./bin/check_java.sh && ./bin/sbt compile test

publish: build
	source ./bin/check_java.sh && ./bin/sbt docker:publishLocal

create: publish
	docker network create app-tier
	docker-compose up --build -d

test: create
	-docker rm rediscache-test
	docker build --pull ./locust -t rediscache-test
	docker run -e TARGET_URL=http://cache:8080 -e REDIS_HOST=redis -e LOCUST_OPTS="--clients=100 -r 10 --no-web --run-time=60" --network app-tier --name rediscache-test rediscache-test:latest

demo: 
	-docker rm rediscache-test
	docker build --pull ./locust -t rediscache-test
	docker run -e TARGET_URL=http://cache:8080 -e REDIS_HOST=redis -e LOCUST_OPTS="--clients=300 -r 10 --no-web --run-time=1200" --network app-tier --name rediscache-test rediscache-test:latest

clean:
	-docker-compose down
	-docker-compose rm
	-docker rm rediscache-test
	-docker network rm app-tier

update-dashboards:
	./update-dashboards.sh

