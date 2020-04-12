package com.bugzmanov

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.bugzmanov.database.RedisClient

class DataSource(redis: RedisClient) {

  def get(key: String): Option[Source[ByteString, _]] = {
    redis.get(key).map { data =>
        Source(List(ByteString.fromArrayUnsafe(data)))
    }
  }
}
