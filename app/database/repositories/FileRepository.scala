package database.repositories

import api.dtos.FileDTO
import database.mappings.FileMappings._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

object FileRepository(db: Database) extends BaseRepository{

  /**
    * Selects all rows from the files table on the database.
    * @return
    */
  def selectAllFiles: Future[Seq[FileRow]] = {
    exec(selectAllFromFilesTable.result)
  }

  /**
    * Deletes all rows from the files table on the database.
    * @return
    */
  def selectFileById(id: Int): Future[Seq[FileRow]] = {
    exec(selectById(id).result)
  }

  /**
    *
    */
  def deleteAllFiles: Future[Int]  = {
    exec(deleteAllFromFilesTable)
  }

  /**
    *
    */
  def deleteFileById(id: Int): Future[Int] = {
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
  def existsCorrespondingFileId(fileId: Int): Future[Boolean] = {
    exec(selectById(fileId).exists.result)

  /**
    * Checks if a corresponding file row exists on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileName(fileName: String): Future[Boolean] = {
    exec(selectByFileName(fileName).exists.result)

  /**
    * Retrieves a fileId of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFileIdFromName(fileName: String): Future[Int] = {
    exec(selectByFileName(fileName).map(_.fileId).result.head)
  }

  /**
    * Retrieves a fileName of a row on the database by providing the fileId.
    * @param fileId Id of the file on the database.
    */
  def selectNameFromFileId(fileId: Int): Future[String] = {
    exec(selectById(fileId).map(_.fileName).result.head)
  }

  /**
    * Retrieves a storageName of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFilePathFromFileName(fileName: String): Future[String] = {
    exec(selectByFileName(fileName).map(_.storageName).result.head)
  }

  /**
    * Retrieves a fileName of a row on the database by providing the storageName.
    * @param storageName Name of the file on the storage folder on the database.
    */
  def selectFileNameFromFilePath(filePath: String): Future[String] = {
    exec(selectByStorageName(filePath).map(_.fileName).result.head)
  }

  /**
    * Method that inserts a file (row) on the files table on the database.
    * @param file FileRow to be inserted on the database.
    */
  def insertInFilesTable(file: FileRow): Unit = {
    exec(insertFile(file))
  }

  /**
    *
    */
  def insertInFilesTable(file: FileDTO): Unit = {
    exec(insertFile(FileRow(0, file.storageName, file.fileName, file.uploadDate)))
  }

}
