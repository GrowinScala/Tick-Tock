package api.controllers

import java.nio.file.Paths

import javax.inject.Singleton
import org.apache.commons.io.FilenameUtils
import play.api.mvc._
import api.dtos.FileDTO
import javax.inject.Inject
import play.api.libs.Files
import play.api.libs.json._

import scala.concurrent.ExecutionContext
import api.utils.DateUtils._
import database.repositories.{FileRepository, TaskRepository}

@Singleton
class FileController @Inject()(cc: ControllerComponents, fileRepo: FileRepository, taskRepo: TaskRepository)(implicit exec: ExecutionContext) extends AbstractController(cc){

  final val MAX_FILE_SIZE = 1024*1024*300 // 300MB

  def index = Action {
    Ok("It works!")
  }

  def upload: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData(MAX_FILE_SIZE)) { request =>
    request.body.file("file").map { file =>
      if(FilenameUtils.getExtension(file.filename) == "jar") {
        val storageName = Paths.get(file.filename).getFileName.toString
        val fileName = request.body.dataParts.head._2.head
        val uploadDate = getCurrentDateTimestamp
        fileRepo.insertInFilesTable(FileDTO(storageName, fileName, uploadDate))
        file.ref.moveTo(Paths.get(s"app/filestorage/$storageName"), replace = false)
        Ok("File uploaded successfully => storageName: " + storageName + ", fileName: " + fileName + ", uploadDate: " + uploadDate)
        //TODO: change StorageName to the UUID
      }
      else BadRequest("File had the wrong extension")
    }.getOrElse {
      BadRequest("File upload went wrong.")
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
  def getFileById(id: Int): Action[AnyContent] = Action.async {
    fileRepo.selectFileById(id).map { seq =>
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
    fileRepo.deleteFileById(id). map { i =>
      if(i > 0) { //TODO - Create file exists and check first
        Ok("File with id = " + id + " as been deleted.")
      } else BadRequest("File with id "+ id+ " does not exist.")
    }
  }


}
