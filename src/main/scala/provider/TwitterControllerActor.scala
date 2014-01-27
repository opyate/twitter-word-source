package provider

import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.Timeout
import core.Queue

/**
 * This class monitors the word queue, which is populated from Twitter.
 * 
 * If the queue runs low (as determined by <code>Min</code>), it spawns a new Twitter instance with which to populate
 * the queue from random samples.
 * 
 * If the queue reaches sufficient capacity (as determined by <code>Max</code>), it kills the Twitter instance.
 * 
 * It does these Min/Max checks every <code>Delay</code> seconds.
 */
object TwitterControllerActor {
  // TODO tweak these based on metrics
  // local testing have run into cases where more words were consumed than the ASCII-filtered Twitter firehose could render.
  val Delay = 5
  val Max = 50000
  val Min = 5000
  
  case object Up
  case object Down
  case object Check
  
  def props(queue: Queue): Props = Props(new TwitterControllerActor(queue))
  
}
class TwitterControllerActor(queue: Queue) extends Actor with ActorLogging {
  
  import TwitterControllerActor._
  import scala.concurrent.duration._
  import context.dispatcher
  
  implicit val timeout = Timeout(2.seconds)
  
  var wordSource: Option[TwitterWordSource] = None
    
  def initWordSource() {
    val wsrc = new TwitterWordSource(queue)
    wsrc.init
    wordSource = Some(wsrc)
  }
  
  override def postStop {
    wordSource foreach(_.shutdown)
    wordSource = None
  }
  
  def receive: Receive = idle
  
  // let's get started...
  context.system.scheduler.scheduleOnce(Delay seconds, self, Check)
  
  def listening: Receive = {
    case Down =>
      log.info("Closing the tap on the Twitter firehose")
      context.become(idle, true)
      wordSource foreach(_.shutdown)
    case Check =>
      queue.size.foreach(size => {
        if (size > Max) {
          self ! Down
          self ! Check
        } else {
          context.system.scheduler.scheduleOnce(Delay seconds, self, Check)
        }
      })
    case _ =>
      log.warning("actor is listening, but received unknown command") 
  }
  
  def idle: Receive = {
    case Up =>
      log.info("Opening the tap on the Twitter firehose")
      context.become(listening, true)
      initWordSource
    case Check =>
      queue.size.foreach(size => {
        if (size < Min) {
          self ! Up
          self ! Check
        } else {
          context.system.scheduler.scheduleOnce(Delay seconds, self, Check)
        }
      })
    case _ =>
      log.warning("actor is idle. Waiting for init")
  }
  
  
}