package database.repositories

import java.util.UUID

import api.dtos.FileDTO
import api.utils.DateUtils._

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
      FileDTO("asd1", "test1", stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")),
      FileDTO("asd2", "test2", stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")),
      FileDTO("asd3", "test3", stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))
    ))
  }

  /**
    *
    */
  def selectFileById(id: String): Future[Option[FileDTO]] = {
    Future.successful(Some(FileDTO("asd1", "test1", stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))))
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
    if(fileName.equals("test1")) Future.successful(true)
    else Future.successful(false)
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
