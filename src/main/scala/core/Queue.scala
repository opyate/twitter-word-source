package core

import com.redis.RedisClient
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future

/**
 * Queue abstraction on top of storage engine (Redis, in our case)
 */
class Queue(db: RedisClient) {

  val QueueName = s"${global.ns}:words"
    
  implicit val timeout = Timeout(2.seconds)
    
  def enqueue(o: String) {
    // we don't care about the return value (the size of the list after insertion)
    db.lpush(QueueName, o)
  }
  
  def dequeue: Future[Option[String]] = {
    db.lpop[String](QueueName)
  }
  
  def size = db.llen(QueueName)
}