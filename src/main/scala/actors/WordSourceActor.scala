package actors

import akka.actor.Actor
import provider.TwitterWordSource
import com.redis.RedisClient
import akka.util.Timeout
import akka.actor.Props
import core.Queue
import akka.actor.ActorLogging

object WordSourceActor {
  case object Get
  
  def props(queue: Queue): Props = Props(new WordSourceActor(queue))
}

class WordSourceActor(queue: Queue) extends Actor with ActorLogging {

  import WordSourceActor._
  import scala.concurrent.duration._
  import context.dispatcher
  import akka.pattern.pipe
  
  implicit val timeout = Timeout(2.seconds)
  
  val analytics = context.actorOf(Props[AnalyticsActor])
  
  def receive: Receive = {
    case Get => {
      val future = queue.dequeue
      future pipeTo sender
      future pipeTo analytics
    }
  }
}