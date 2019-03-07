package api.controllers

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import javax.inject.Singleton
import org.apache.commons.io.FilenameUtils
import play.api.mvc._
import javax.inject.Inject
import play.api.libs.json._

import api.dtos.FileDTO
import api.utils.DateUtils._
import api.utils.UUIDGenerator

import scala.concurrent.{ExecutionContext, Future}
import database.repositories.{FileRepository, TaskRepository}
import api.validators.Error._

import api.utils.ErrorMessages._

@Singleton
class FileController @Inject()(cc: ControllerComponents)(implicit exec: ExecutionContext, implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val UUIDGen: UUIDGenerator) extends AbstractController(cc) {

  def index = Action {
    Ok("It works!")
  }

  def upload: Action[AnyContent] = Action.async { request =>
    request.body.asMultipartFormData.get.file("file").map{
      file =>
        if(FilenameUtils.getExtension(file.filename) == "jar") {
          val uuid = UUIDGen.generateUUID
          val fileName = request.body.asMultipartFormData.get.dataParts.head._2.head
          val uploadDate = getCurrentDateTimestamp
          fileRepo.existsCorrespondingFileName(fileName).map{elem =>
            if(elem) Future.successful(BadRequest(Json.toJsObject(invalidUploadFileName)))
          }
          //TODO: Add these to config instead of hardcoding them. Play has an "application.conf" when you can store configs that then load on "run-time". Learn how to do it ;)
          val initialFilePath = Paths.get(s"C:/Users/Pedro/Desktop/$uuid")
          val finalFilePath = Paths.get(s"app/filestorage/$uuid" + ".jar")
          file.ref.moveTo(initialFilePath, replace = false)
          Files.move(initialFilePath, finalFilePath, StandardCopyOption.ATOMIC_MOVE)
          fileRepo.insertInFilesTable(FileDTO(uuid, fileName, uploadDate))
          //TODO: When Links are implemented, send back a "link to self".
          Future.successful(Ok("File uploaded successfully => fileId: " + uuid + ", fileName: " + fileName + ", uploadDate: " + uploadDate))
        }
        else Future.successful(BadRequest(Json.toJsObject(invalidFileExtension)))
    }.getOrElse {
      Future.successful(BadRequest(Json.toJsObject(invalidUploadFormat)))
    }
  }

  /**
    * Method that retrieves all files in the database
    *
    * @return a list containing all the files in the database
    */
  def getAllFiles: Action[AnyContent] = Action.async {
    fileRepo.selectAllFiles.map { seq =>
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
  //TODO: Considering you're using Options, pattern matching is considered more "Scala" than the if/else.

  def getFileById(id: String): Action[AnyContent] = Action.async {
    fileRepo.selectFileById(id).map{ tr =>
      if (tr.isDefined)
        Ok(Json.toJsObject(tr.get))
      else
      //Carefully pick messages so that any user trying to exploit your service can't infer from them. "does not exits" tells the user that it does not exist
      //"does not belong to you" tells the user it exists, but in another user "not found" is ambiguous, user can't infer "why" it could not be found. ;)
        BadRequest(fileNotFoundError(id))
    }
  }

  /**
    * Method that deletes the file with the given id
    *
    * @param id - identifier of the file to be deleted
    * @return HTTP response Ok if the file was deleted and BadRequest if not
    */
  def deleteFile(id: String): Action[AnyContent] = Action.async {
    fileRepo.deleteFileById(id).map { i =>
      if(i > 0) { //TODO - Create file exists and check first
        //usually, for the DELETE the response is 204 NO CONTENT. Ok isn't wrong, just not according the standards.
        Ok("File with id = " + id + " has been deleted.")
      } else BadRequest("File with id " + id + " does not exist.")
    }
  }


}
