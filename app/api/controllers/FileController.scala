package controllers

import api.dtos.FileDTO
import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import api.services.FileService._
import database.repositories.FileRepository._
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class FileController @Inject()(cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  /**
    * Method that retrieves all files in the database
    *
    * @return a list containing all the files in the database
    */
  def getAllFiles: Action[AnyContent] = Action.async {
    selectAllFiles.map { seq =>
      val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
      Ok(result)
    }
  }

  /**
    *
    * @param id
    * @return
    */
  def getFileById(id: Int): Action[AnyContent] = Action.async {
   selectFileById(id).map { seq =>
     val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
     Ok(result)
   }
  }

}
