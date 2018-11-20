package controllers

import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import services.TickTock
import play.api.libs.functional.syntax._

class TaskController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  object ScheduledTask {

    implicit val scheduledTaskReads: Reads[ScheduledTask] = (
      (JsPath \ "startDateAndTime").read[String] and
        (JsPath \ "task").read[String]
      )(ScheduledTask.apply _)
    implicit val scheduledTaskWrites: Writes[ScheduledTask] = (
      (JsPath \ "startDateAndTime").write[String] and
        (JsPath \ "task").write[String]
      )(unlift(ScheduledTask.unapply))
  }

  case class ScheduledTask(startDateAndTime: String, task: String)

  def index = Action {
    Ok("It works!")
  }

  def schedule: Action[JsValue] = Action(parse.json) { request =>
    val json = request.body
    TickTock.scheduleOnce((json \ "task").as[String], (json \ "startDateAndTime").as[String])
    Ok(Json.obj("result" -> json))
  }

}
