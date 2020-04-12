from locust import HttpLocust, TaskSet, between
import random
import string
import redis
import os

def small(l):
    l.client.get("/v1/small_%i" % random.randrange(1, 1000), name="small")

def medium(l):
    l.client.get("/v1/medium_%i" % random.randrange(1, 100), name="medium")

def large(l):
    l.client.get("/v1/large_%i" % random.randrange(1, 10), name="large")

def random_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))

def alwaysmiss(l):
    with l.client.get("/v1/" + random_generator(10), name="always miss", catch_response=True) as response:
        if response.status_code == 404:
            response.success()


redis_host = os.getenv('REDIS_HOST', 'localhost')

r = redis.StrictRedis(host=redis_host, port=6379)

if not r.exists("small_500") or not r.exists("medium_50") or not r.exists("large_5"):
    print("Loading test data into redis. This might take some time")

    for x in range(1000):
        r.set("small_%i" % x, random_generator(1024))

    print("10% complete")

    for x in range(100):
        r.set("medium_%i" % x, random_generator(1024 * 1024))

    print("70% complete")

    large_data = random_generator(50*1024 * 1024)

    for x in range(10):
        r.set("large_%i" % x, large_data)

    print("Loading complete")

def largest_test(l):
    r = redis.StrictRedis(host="localhost", port=6379)
    r.delete("large_%i" % i )
    i = i + 1
    r.set("large_%i" % i, random_generator(1024 * 1024 * 100))
    l.client.get("/v1/large_%i" % i, name="large")


class UserBehavior(TaskSet):
    tasks = {small: 20, medium: 10, large: 1, alwaysmiss:10}

class WebsiteUser(HttpLocust):
    task_set = UserBehavior
    wait_time = between(0.0, 2.0)

# class LargeTaskSet(TaskSet):
#     tasks = {largest_test: 1}
#
# class Large(HttpLocust):
#     task_set = LargeTaskSet
#     wait_time = between(30.0, 90.0)
