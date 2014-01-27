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
    db.incr("count")
    
    // bump the count for the word
    incRankings("topWords", word)
    
    // bump the count for the individual letters
    word.foreach { letter => incRankings("topLetters", letter.toString)}
  }
  
  /**
   * item => a word or letter that needs to be scored
   */
  private [this] def incRankings(setName: String, item: String) {
    // bump the count for the item
    db.incr(item)
    
    // add the word to the top list by its score
    val wordScore = db.get(item)
    wordScore.onSuccess {
      case Some(score) => {
        db.zadd(setName, score.toDouble, item)
      }
    }
  }
}