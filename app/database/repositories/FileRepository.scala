package database.repositories

import java.util.UUID

import api.dtos.FileDTO
import scala.concurrent.Future

trait FileRepository {

  /**
   * Selects all rows from the files table on the database.
   *
   * @return all files in the database
   */
  def selectAllFiles: Future[Seq[FileDTO]]

  /**
   * Selects a file from the database given an id
   *
   * @return A fileDTO containing the selected file
   */
  def selectFileById(id: String): Future[Option[FileDTO]]

  /**
   * Deletes all rows from the files table on the database.
   *
   * @return An Int representing the number of rows deleted.
   */
  def deleteAllFiles: Future[Int]

  /**
   * Deletes a single file from the database given its id
   */
  def deleteFileById(id: String): Future[Int]

  //TODO: CREATE,ALTER,DELETE tasks should not be provided as part of the interface.
  /*/**
    * Creates the files table on the database.
    */
  def createFilesTable: Future[Unit]

  /**
    * Drops the files table on the database.
    */
  def dropFilesTable: Future[Unit]*/

  /**
   * Checks if a corresponding file row exists on the database by providing its fileId.
   *
   * @param fileId Id of the file on the database.
   * @return true if row exists, false if not.
   */
  def existsCorrespondingFileId(fileId: String): Future[Boolean]

  /**
   * Checks if a corresponding file row exists on the database by providing the fileName.
   *
   * @param fileName Name of the file given by the user on the database.
   * @return true if row exists, false if not.
   */
  def existsCorrespondingFileName(fileName: String): Future[Boolean]

  /**
   * Retrieves a fileId of a row on the database by providing the fileName.
   *
   * @param fileName Name of the file given by the user on the database.
   */
  def selectFileIdFromFileName(fileName: String): Future[String]

  /**
   * Retrieves a fileName of a row on the database by providing the fileId.
   *
   * @param fileId Id of the file on the database.
   */
  def selectFileNameFromFileId(fileId: String): Future[String]

  /**
   * Method that inserts a file (row) on the files table on the database.
   *
   * @param file FileDTO to be inserted on the database.
   */
  def insertInFilesTable(file: FileDTO): Future[Boolean]
}
