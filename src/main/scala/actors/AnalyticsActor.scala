package actors

import com.redis.RedisClient
import akka.actor.Actor
import akka.util.Timeout

class AnalyticsActor extends Actor {
  
  import scala.concurrent.duration._
  import context.dispatcher
  
  implicit val timeout = Timeout(2.seconds)

  // Redis client setup
  val db = RedisClient("localhost", 6379)
 
  def receive: Receive = {
    case Some(word: String) => record(word)
  }
  
  private [this] def record(word: String) {
    // bump the overall count
    db.incr(s"${global.ns}:count")
    
    // bump the count for the word
    db.zincrby(s"${global.ns}:topWords", 1, word)
    
    // bump the count for the individual letters
    word.foreach { letter => db.zincrby(s"${global.ns}:topLetters", 1, letter.toString)}
  }
}