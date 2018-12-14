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
    * Method that returns the file with the given id
    *
    * @param id - identifier of the file we are looking for
    * @return the file corresponding to the id given
    */
  def getFileById(id: Int): Action[AnyContent] = Action.async {
   selectFileById(id).map { seq =>
     val result = JsArray(seq.map(tr => Json.toJsObject(tr)))
     Ok(result)
   }
  }

  /**
    * Method that deletes the file with the given id
    *
    * @param id - identifier of the file to be deleted
    * @return HTTP response Ok if the file was deleted and BadRequest if not
    */
  def deleteFile(id: Int): Action[AnyContent] = Action.async {
    deleteFileById(id). map { i =>
      if(i == 1) { //TODO - Maybe change this here??
        Ok("File with id = " + id + " as been deleted.")
      } else BadRequest("File with id "+ id+ " does not exist.")
    }
  }

}
