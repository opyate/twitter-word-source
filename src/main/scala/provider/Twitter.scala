package provider

import twitter4j.StatusDeletionNotice
import twitter4j.StatusListener
import twitter4j.StallWarning
import twitter4j.Status
import twitter4j.TwitterStreamFactory
import models.WordSource
import akka.actor.ActorRef
import com.redis.RedisClient
import core.Queue
import scala.concurrent.future
import scala.concurrent.ExecutionContext

class TwitterWordSource(queue: Queue)(implicit executionContext: ExecutionContext) extends WordSource {
  
  override def next_word = try {
    queue.dequeue
  } catch {
    case e: Throwable => {
      shutdown
      future { None }
    }
  }

  lazy val twitterStream = new TwitterStreamFactory(config).getInstance
  
  def init() = {
    twitterStream.addListener(simpleStatusListener(queue))
    twitterStream.sample 
  }
  
  override def shutdown = {
    twitterStream.cleanUp
    twitterStream.shutdown
  }

  lazy val config = new twitter4j.conf.ConfigurationBuilder()
    .setOAuthConsumerKey(System.getenv("TWITTER_KEY"))
    .setOAuthConsumerSecret(System.getenv("TWITTER_SECRET"))
    .setOAuthAccessToken(System.getenv("TWITTER_ACCESS_TOKEN"))
    .setOAuthAccessTokenSecret(System.getenv("TWITTER_ACCESS_TOKEN_SECRET"))
    .build

  def simpleStatusListener(queue: Queue) = new StatusListener() {
    //val ASCII2LettersOrMorePattern = "([\\x00-\\x7F]{2,})".r
    val TwoOrMoreLettersPattern = "([a-zA-Z]{2,})".r
    
    def onStatus(status: Status) {
      status.getText split("\\s+") foreach { word => word match {
        case TwoOrMoreLettersPattern(word) => queue.enqueue(word)
        case _ => // discard
      }}
    }
    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
    def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
    def onException(ex: Exception) { ex.printStackTrace }
    def onScrubGeo(arg0: Long, arg1: Long) {}
    def onStallWarning(warning: StallWarning) {}
  }
}
