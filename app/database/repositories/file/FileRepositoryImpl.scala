package database.repositories.file

import java.util.Date

import api.dtos.FileDTO
import api.utils.DateUtils
import database.mappings.FileMappings._
import javax.inject.Inject
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ ExecutionContext, Future }

class FileRepositoryImpl @Inject() (dtbase: Database) extends FileRepository {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def fileRowToFileDTO(file: FileRow): FileDTO = {
    FileDTO(file.fileId, file.fileName, file.uploadDate)
  }

  /**
   * Selects all rows from the files table on the database.
   * @return
   */
  def selectAllFiles(offset: Option[Int] = None, limit: Option[Int] = None): Future[Seq[FileDTO]] = {

    val files = if (offset.isDefined && limit.isDefined)
      selectAllFromFilesTable.drop(offset.get).take(limit.get)
    else selectAllFromFilesTable

    dtbase.run(files.result).map { seq =>
      seq.map(elem => FileDTO(elem.fileId, elem.fileName, elem.uploadDate))
    }

  }

  def selectFileById(id: String): Future[Option[FileDTO]] = {
    dtbase.run(getFileByFileId(id).result.headOption).map {
      case Some(value) => Some(fileRowToFileDTO(value))
      case None => None
    }
  }

  def selectFileByName(name: String): Future[Option[FileDTO]] = {
    dtbase.run(getFileByFileName(name).result.headOption).map {
      case Some(value) => Some(fileRowToFileDTO(value))
      case None => None
    }
  }

  /**
   * Deletes all rows from the files table on the database.
   * @return
   */
  def deleteAllFiles: Future[Int] = {
    dtbase.run(deleteAllFromFilesTable)
  }

  def deleteFileById(id: String): Future[Int] = {
    dtbase.run(deleteFileByFileId(id))
  }

  /**
   * Checks if a corresponding file row exists on the database by providing its fileId.
   * @param fileId Id of the file on the database.
   * @return true if row exists, false if not.
   */
  def existsCorrespondingFileId(fileId: String): Future[Boolean] = {
    selectFileById(fileId).map(elem => elem.isDefined)
  }

  /**
   * Checks if a corresponding file row exists on the database by providing the fileName.
   * @param fileName Name of the file given by the user on the database.
   * @return true if row exists, false if not.
   */
  def existsCorrespondingFileName(fileName: String): Future[Boolean] = {
    selectFileByName(fileName).map(elem => elem.isDefined)
  }

  /**
   * Retrieves a fileId of a row on the database by providing the fileName.
   * @param fileName Name of the file given by the user on the database.
   */
  def selectFileIdFromFileName(fileName: String): Future[String] = {
    dtbase.run(getFileByFileName(fileName)
      .result
      .headOption
      .map(row =>
        row.getOrElse(FileRow("", "", DateUtils.removeTimeFromDate(new Date())))))
      .map(_.fileId)
  }

  /**
   * Retrieves a fileName of a row on the database by providing the fileId.
   * @param fileId Id of the file on the database.
   */
  def selectFileNameFromFileId(fileId: String): Future[String] = {
    dtbase.run(getFileByFileId(fileId)
      .result
      .headOption
      .map(row =>
        row.getOrElse(FileRow("", "", DateUtils.removeTimeFromDate(new Date())))))
      .map(_.fileName)
  }

  /**
   * Method that inserts a file (row) on the files table on the database.
   * @param file FileDTO to be inserted on the database.
   */
  def insertInFilesTable(file: FileDTO): Future[Boolean] = {
    dtbase.run(insertFile(FileRow(file.fileId, file.fileName, file.uploadDate))
      .map(numberInsertions => numberInsertions == 1))
  }

}
