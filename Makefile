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
	docker run -e TARGET_URL=http://cache:8080 -e REDIS_HOST=redis --network app-tier --name rediscache-test rediscache-test:latest -f /mnt/locustfile.py --headless --host http://cache:8080 --users=100 --hatch-rate 10 --run-time 60

demo: 
	-docker rm rediscache-test
	docker build --pull ./locust -t rediscache-test
	docker run -e TARGET_URL=http://cache:8080 -e REDIS_HOST=redis --network app-tier --name rediscache-test rediscache-test:latest -f /mnt/locustfile.py --headless --host http://cache:8080 --users=100 --hatch-rate 10 --run-time 600

clean:
	-docker-compose down
	-docker-compose rm
	-docker rm rediscache-test
	-docker network rm app-tier

update-dashboards:
	./update-dashboards.sh

