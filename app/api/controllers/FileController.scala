package controllers

import api.dtos.FileDTO
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import api.services.FileService._
import database.repositories.FileRepository._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class FileController @Inject()(cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def getAllFiles: Action[AnyContent] = Action.async {
    selectAllFiles.map { seq =>
      val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
      Ok(result)
    }
  }
}
