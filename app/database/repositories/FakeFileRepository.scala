package database.repositories

import java.util.UUID

import api.dtos.FileDTO
import api.utils.DateUtils._
import slick.dbio.DBIO

import scala.concurrent.Future

class FakeFileRepository extends FileRepository{

  implicit val fileRepo: FileRepository = this

  /**
    * Selects all rows from the files table on the database.
    *
    * @return
    */
  def selectAllFiles: Future[Seq[FileDTO]] = {
    Future.successful(Seq(
      FileDTO("asd1", "test1", getCurrentDateTimestamp),
      FileDTO("asd2", "test2", getCurrentDateTimestamp),
      FileDTO("asd3", "test3", getCurrentDateTimestamp)
    ))
  }

  /**
    *
    */
  def selectFileById(id: String): Future[FileDTO] = {
    Future.successful(FileDTO("asd1", "test1", getCurrentDateTimestamp))
  }

  /**
    * Deletes all rows from the files table on the database.
    *
    * @return
    */
  def deleteAllFiles: Future[Int] = {
    Future.successful(3)
  }

  /**
    *
    */
  def deleteFileById(id: String): Future[Int] = {
    Future.successful(1)
  }

  /**
    * Creates the files table on the database.
    */
  def createFilesTable: Future[Unit] = {
    Future.successful((): Unit)
  }

  /**
    * Drops the files table on the database.
    */
  def dropFilesTable: Future[Unit] = {
    Future.successful((): Unit)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing its fileId.
    *
    * @param fileId Id of the file on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileId(fileId: String): Future[Boolean] = {
    Future.successful(true)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing the fileName.
    *
    * @param fileName Name of the file given by the user on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileName(fileName: String): Future[Boolean] = {
    Future.successful(true)
  }

  /**
    * Retrieves a fileId of a row on the database by providing the fileName.
    *
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFileIdFromFileName(fileName: String): Future[String] = {
    Future.successful("asd1")
  }

  /**
    * Retrieves a fileName of a row on the database by providing the fileId.
    *
    * @param fileId Id of the file on the database.
    */
  def selectFileNameFromFileId(fileId: String): Future[String] = {
    Future.successful("test1")
  }


  /**
    * Method that inserts a file (row) on the files table on the database.
    *
    * @param file FileDTO to be inserted on the database.
    */
  def insertInFilesTable(file: FileDTO): Future[Boolean] = {
    Future.successful(true)
  }
}
