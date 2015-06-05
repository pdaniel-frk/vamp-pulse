package io.vamp.pulse.elasticsearch

import akka.actor.{FSM, _}
import io.vamp.common.akka.Bootstrap.Start
import io.vamp.common.akka._
import io.vamp.common.http.RestClient
import io.vamp.pulse.elasticsearch.ElasticsearchInitializationActor.{DoneWithOne, WaitForOne}
import io.vamp.pulse.notification._

import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ElasticsearchInitializationActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticsearchInitializationActor]

  sealed trait InitializationEvent

  object WaitForOne extends InitializationEvent

  object DoneWithOne extends InitializationEvent

}

sealed trait State

case object Idle extends State

case object Active extends State

case object Done extends State

class ElasticsearchInitializationActor extends FSM[State, Int] with CommonActorSupport with PulseNotificationProvider {

  import ElasticsearchActor._

  private lazy val templates = {
    def load(name: String) = Source.fromInputStream(getClass.getResourceAsStream(s"$name.json")).mkString.replace("$NAME", defaultIndexName)
    List("template", "template-event").map(template => s"$defaultIndexName-$template" -> load(template)).toMap
  }

  startWith(Idle, 0)

  when(Idle) {
    case Event(Start, 0) =>
      initializeTemplates()
      goto(Active) using 1
  }

  when(Active, stateTimeout = timeout.duration) {
    case Event(WaitForOne, count) => stay() using count + 1

    case Event(DoneWithOne, count) => if (count > 1) stay() using count - 1 else done()

    case Event(StateTimeout, _) =>
      exception(ElasticsearchInitializationTimeoutError)
      done()
  }

  when(Done) {
    case _ => stay()
  }

  initialize()

  def done() = {
    actorFor(ElasticsearchActor) ! ElasticsearchActor.StartIndexing
    goto(Done) using 0
  }

  private def initializeTemplates() = {
    val receiver = self

    def createTemplate(name: String) = templates.get(name).foreach { template =>
      receiver ! WaitForOne
      RestClient.request[Any](s"PUT $restApiUrl/_template/$name", template) onComplete {
        case _ => receiver ! DoneWithOne
      }
    }

    RestClient.request[Any](s"GET $restApiUrl/_template", None, "", { case field => field }) onComplete {
      case Success(response) =>
        response match {
          case map: Map[_, _] => templates.keys.filterNot(name => map.asInstanceOf[Map[String, Any]].contains(name)).foreach(createTemplate)
          case _ => templates.keys.foreach(createTemplate)
        }
        receiver ! DoneWithOne

      case Failure(t) =>
        log.warning(s"Failed to do part of Elasticsearch initialization: $t")
        receiver ! DoneWithOne
    }
  }
}
