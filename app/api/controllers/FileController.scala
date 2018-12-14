package api.controllers

import java.io.File
import java.nio.file.Paths
import java.sql.Timestamp
import java.util.Calendar
import api.services.FileService._
import api.dtos.FileDTO
import database.repositories.{FileRepository, TaskRepository}
import javax.inject.{Inject, Singleton}
import org.apache.commons.io.FilenameUtils
import slick.jdbc.MySQLProfile.api._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.mvc.{Http, Result}

@Singleton
class FileController @Inject()(cc: ControllerComponents) extends AbstractController(cc){

  final val MAX_FILE_SIZE = 1024*1024*300 // 300MB

  val db = Database.forConfig("dbinfo")
  val fileRepo = new FileRepository(db)
  val taskRepo = new TaskRepository(db)

  def index = Action {
    Ok("It works!")
  }

  def upload = Action(parse.multipartFormData(MAX_FILE_SIZE)) { request =>

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


}
