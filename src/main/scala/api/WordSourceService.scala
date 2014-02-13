package api

import scala.concurrent.ExecutionContext
import spray.routing.Directives
import spray.http.MediaTypes.{ `application/json` }
import akka.actor.ActorRef
import akka.util.Timeout
import spray.httpx.SprayJsonSupport

class WordSourceService(wordsource: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with SprayJsonSupport {

  import actors.WordSourceActor._
  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(2.seconds)
  
  val route = {
    pathPrefix("api") {
      path("words") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              (wordsource ask Get).mapTo[Option[String]]
            }
          }
        }
      }
    }
  }
}
