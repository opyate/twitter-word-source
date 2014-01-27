package models

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.future

/**
 * @author Juan M Uys <opyate@gmail.com>
 *
 */
trait WordSource {
  def next_word(): Future[Option[String]]
  def shutdown()
}

class StringWordSource(words: String*)(implicit executionContext: ExecutionContext) extends WordSource {
  override def next_word() = future { Some("test") }
  override def shutdown() { }
}
