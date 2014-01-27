package api

import scala.concurrent.ExecutionContext
import spray.routing.Directives
import spray.http.MediaTypes.{ `application/json` }
import akka.actor.ActorRef
import akka.util.Timeout
import com.redis.RedisClient
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import com.redis.serialization.SprayJsonSupport._

class AnalyticsService(db: RedisClient)(implicit executionContext: ExecutionContext) extends Directives  {
  
  import scala.concurrent.duration._
  implicit val timeout = Timeout(2.seconds)

  val route = {
    pathPrefix("api") {
      path("count") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val x = db.get("count")
              x
            }
          }
        }
      } ~
      path("top" ~ "\\d+".r ~ "words") { num =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val x = db.zrevrange("topWords", 0, num.toInt - 1)
              x
            }
          }
        }
      } ~
      path("top" ~ "\\d+".r ~ "letters") { num =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val x = db.zrevrange("topLetters", 0, num.toInt - 1)
              x
            }
          }
        }
      }
    }
  }
}