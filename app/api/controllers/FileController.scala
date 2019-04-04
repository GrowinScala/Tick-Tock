package api.controllers

import java.nio.file.{ Files, Paths, StandardCopyOption }

import api.dtos.FileDTO
import api.utils.DateUtils._
import api.utils.UUIDGenerator
import api.validators.Error._
import com.typesafe.config.ConfigFactory
import database.repositories.{ FileRepository, TaskRepository }
import javax.inject.{ Inject, Singleton }
import org.apache.commons.io.FilenameUtils
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class FileController @Inject() (cc: ControllerComponents)(implicit exec: ExecutionContext, implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val UUIDGen: UUIDGenerator) extends AbstractController(cc) {

  private val conf = ConfigFactory.load()

  def index: Action[AnyContent] = Action {
    Ok("It works!")
  }

  def upload: Action[AnyContent] = Action.async { request =>
    request.body.asMultipartFormData.get.file("file").map {
      file =>
        if (FilenameUtils.getExtension(file.filename) == "jar") {
          val uuid = UUIDGen.generateUUID
          val fileName = request.body.asMultipartFormData.get.dataParts.head._2.head
          val uploadDate = getCurrentDateTimestamp
          fileRepo.existsCorrespondingFileName(fileName).map { elem =>
            if (elem) Future.successful(BadRequest(Json.toJsObject(invalidUploadFileName)))
          }
          val initialFilePath = Paths.get(conf.getString("initialFilePath") + uuid)
          val finalFilePath = Paths.get(conf.getString("finalFilePath") + uuid + ".jar")
          file.ref.moveTo(initialFilePath, replace = false)
          Files.move(initialFilePath, finalFilePath, StandardCopyOption.ATOMIC_MOVE)
          fileRepo.insertInFilesTable(FileDTO(uuid, fileName, uploadDate))
          val url = routes.FileController.getFileById(uuid).absoluteURL(request.secure)(request).stripSuffix("/").trim
          Future.successful(Ok("File uploaded successfully => " + url))
        } else Future.successful(BadRequest(Json.toJsObject(invalidFileExtension)))
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
  def getFileById(id: String): Action[AnyContent] = Action.async {
    fileRepo.selectFileById(id).map {
      case Some(file) => Ok(Json.toJsObject(file))
      case None => BadRequest(Json.toJsObject(invalidFileName))
    }
  }

  /**
   * Method that deletes the file with the given id
   *
   * @param id - identifier of the file to be deleted
   * @return HTTP response Ok if the file was deleted and BadRequest if not
   */
  def deleteFile(id: String): Action[AnyContent] = Action.async {
    fileRepo.selectFileById(id).map {
      case Some(_) =>
        fileRepo.deleteFileById(id); NoContent
      case None => BadRequest(Json.toJsObject(invalidEndpointId))
    }
  }

}
