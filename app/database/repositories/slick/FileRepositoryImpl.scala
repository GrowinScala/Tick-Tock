package database.repositories.slick

import java.util.UUID

import api.dtos.FileDTO
import database.mappings.FileMappings._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import database.repositories.FileRepository

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class FileRepositoryImpl(dtbase: Database) extends FileRepository{

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def exec[T](action: DBIO[T]): Future[T] = dtbase.run(action)

  /**
    * Selects all rows from the files table on the database.
    * @return
    */
  def selectAllFiles: Future[Seq[FileDTO]] = {
    exec(selectAllFromFilesTable.result).map{seq =>
      seq.map(elem => FileDTO(elem.fileId, elem.fileName, elem.uploadDate))
    }
  }

  /**
    *
    */
  def selectFileById(id: String): Future[Seq[FileDTO]] = {
    exec(selectById(id).result).map{
      seq => seq.map {
        elem => FileDTO(elem.fileId, elem.fileName, elem.uploadDate)
      }
    }
  }

  /**
    * Deletes all rows from the files table on the database.
    * @return
    */
  def deleteAllFiles: Future[Int] = {
    exec(deleteAllFromFilesTable)
  }

  /**
    *
    */
  def deleteFileById(id: String): Future[Int] = {
    exec(deleteById(id))
  }

  /**
    * Creates the files table on the database.
    */
  def createFilesTable: Unit = {
    exec(createFilesTableAction)
  }

  /**
    * Drops the files table on the database.
    */
  def dropFilesTable: Unit = {
    exec(dropFilesTableAction)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing its fileId.
    * @param fileId Id of the file on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileId(fileId: String): Future[Boolean] = {
    exec(selectById(fileId).exists.result)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileName(fileName: String): Future[Boolean] = {
    exec(selectByFileName(fileName).exists.result)
  }

  /**
    * Retrieves a fileId of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFileIdFromName(fileName: String): Future[String] = {
    exec(selectByFileName(fileName).result.head.map(_.fileId))
  }

  /**
    * Retrieves a fileName of a row on the database by providing the fileId.
    * @param fileId Id of the file on the database.
    */
  def selectFileNameFromFileId(fileId: String): Future[String] = {
    exec(selectById(fileId).map(_.fileName).result.head)
  }

  /*
  /**
    * Retrieves a storageName of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectStorageNameFromFileName(fileName: String): Future[String] = {
    exec(selectByFileName(fileName).map(_.storageName).result.head)
  }

  /**
    * Retrieves a fileName of a row on the database by providing the storageName.
    * @param storageName Name of the file on the storage folder on the database.
    */
  def selectFileNameFromStorageName(storageName: String): Future[String] = {
    exec(selectByStorageName(storageName).map(_.fileName).result.head)
  }
  */

  /**
    * Method that inserts a file (row) on the files table on the database.
    * @param file FileDTO to be inserted on the database.
    */
  def insertInFilesTable(file: FileDTO): Unit = {
    exec(insertFile(FileRow(file.fileId, file.fileName, file.uploadDate)))
  }

}
