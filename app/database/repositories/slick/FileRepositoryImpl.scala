package database.repositories.slick

import api.dtos.FileDTO
import database.mappings.FileMappings._
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._
import database.repositories.FileRepository

import scala.concurrent.Await
import scala.concurrent.duration._

class FileRepositoryImpl(dtbase: Database) extends FileRepository{

  def exec[T](action: DBIO[T]): T = Await.result(dtbase.run(action), 2 seconds)

  /**
    * Selects all rows from the files table on the database.
    * @return
    */
  def selectAllFiles: Seq[FileDTO] = {
    val row = exec(selectAllFromFilesTable.result)
    row.map(elem => FileDTO(elem.fileName, elem.storageName, elem.uploadDate))
  }

  /**
    * Deletes all rows from the files table on the database.
    * @return
    */
  def deleteAllFiles: Int  = {
    exec(deleteAllFromFilesTable)
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
  def existsCorrespondingFileId(fileId: Int): Boolean = {
    exec(selectById(fileId).result) != Vector()
  }

  /**
    * Checks if a corresponding file row exists on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileName(fileName: String): Boolean = {
    exec(selectByFileName(fileName).result) != Vector()
  }

  /**
    * Retrieves a fileId of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFileIdFromName(fileName: String): Int = {
    exec(selectByFileName(fileName).map(_.fileId).result.head)
  }

  /**
    * Retrieves a fileName of a row on the database by providing the fileId.
    * @param fileId Id of the file on the database.
    */
  def selectFileNameFromFileId(fileId: Int): String = {
    exec(selectById(fileId).map(_.fileName).result.head)
  }

  /**
    * Retrieves a storageName of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectStorageNameFromFileName(fileName: String): String = {
    exec(selectByFileName(fileName).map(_.storageName).result.head)
  }

  /**
    * Retrieves a fileName of a row on the database by providing the storageName.
    * @param storageName Name of the file on the storage folder on the database.
    */
  def selectFileNameFromStorageName(storageName: String): String = {
    exec(selectByStorageName(storageName).map(_.fileName).result.head)
  }

  /**
    * Method that inserts a file (row) on the files table on the database.
    * @param file FileDTO to be inserted on the database.
    */
  def insertInFilesTable(file: FileDTO): Unit = {
    exec(insertFile(FileRow(0, file.storageName, file.fileName, file.uploadDate)))
  }

}
