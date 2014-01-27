package core

import actors.WordSourceActor
import akka.actor.ActorSystem
import akka.actor.Props
import com.redis.RedisClient
import provider.TwitterControllerActor

/**
 * Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
 * apps as well as in our tests.
 */
trait Core {

  implicit def system: ActorSystem

}

/**
 * This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
 * termination handler to stop the system when the JVM exits.
 */
trait BootedCore extends Core {

  /**
   * Construct the ActorSystem we will use in our application
   */
  implicit lazy val system = ActorSystem("wordsource")

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook(system.shutdown())

}

/**
 * This trait contains the actors that make up our application; it can be mixed in with
 * ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
 * 
 * db, the Redis instance to use "as-is"
 * queue, the queue abstraction based on Redis
 */
trait CorePlumbing {
  this: Core =>
   
  val db = RedisClient("localhost", 6379)
  val queue = new Queue(db)
  
  val wordsource = system.actorOf(WordSourceActor.props(queue))
  val twitterController = system.actorOf(TwitterControllerActor.props(queue))
}