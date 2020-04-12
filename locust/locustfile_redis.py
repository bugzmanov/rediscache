from locust import Locust, HttpLocust, TaskSet, between,events
import random
import string
import redis
import time
from locust.core import TaskSet, task

def small(l):
    l.client.query("small_%i" % random.randrange(1, 1000), name="small")

def medium(l):
    l.client.query("medium_%i" % random.randrange(1, 100), name="medium")

def large(l):
    l.client.query("blob", name="large")

def random_generator(size=6, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))

# def alwaysmiss(l):
#     with l.client.query("" + random_generator(10), name="always miss") as response:
#         if response.status_code == 404:
#             response.success()



class RedisClient(object):
    def __init__(self, host="localhost", port=6379):
        self.rc = redis.StrictRedis(host=host, port=port)

    def query(self, key, name, command='GET'):
        result = None
        start_time = time.time()
        try:
            result = self.rc.get(key)
            if not result:
                result = ''
        except Exception as e:
            total_time = int((time.time() - start_time) * 1000)
            events.request_failure.fire(request_type=command, name=name, response_time=total_time, exception=e)
        else:
            total_time = int((time.time() - start_time) * 1000)
            length = len(result)
            events.request_success.fire(request_type=command, name=name, response_time=total_time,
                                        response_length=length)
        return result


class RedisLocust(Locust):
    def __init__(self, *args, **kwargs):
        super(RedisLocust, self).__init__(*args, **kwargs)
        self.client = RedisClient()


# print("ololo")
# r = redis.StrictRedis(host="localhost", port=6379)
# for x in range(1000):
#     r.set("small_%i" % x, random_generator(1024))
#
# for x in range(100):
#     r.set("medium_%i" % x, random_generator(1024 * 1024))

class UserBehavior(TaskSet):
    tasks = {small: 10, medium: 5, large: 2}

class WebsiteUser(RedisLocust):
    task_set = UserBehavior
    wait_time = between(5.0, 9.0)
