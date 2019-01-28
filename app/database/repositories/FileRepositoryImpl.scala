package database.repositories

import api.dtos.FileDTO
import database.mappings.FileMappings._
import javax.inject.Inject
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class FileRepositoryImpl @Inject() (dtbase: Database) extends FileRepository{

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private def fileRowToFileDTO(file: FileRow) = {
    FileDTO(file.fileId, file.fileName, file.uploadDate)
  }

  /**
    * Selects all rows from the files table on the database.
    * @return
    */
  def selectAllFiles: Future[Seq[FileDTO]] = {
    dtbase.run(selectAllFromFilesTable.result).map{seq =>
      seq.map(elem => FileDTO(elem.fileId, elem.fileName, elem.uploadDate))
    }
  }

  /**
    *
    */
  def selectFileById(id: String): Future[FileDTO] = {
    dtbase.run(selectById(id).result).map(seq => fileRowToFileDTO(seq.head))
  }

  /**
    * Deletes all rows from the files table on the database.
    * @return
    */
  def deleteAllFiles: Future[Int] = {
    dtbase.run(deleteAllFromFilesTable)
  }

  /**
    *
    */
  def deleteFileById(id: String): Future[Int] = {
    dtbase.run(deleteById(id))
  }

  /**
    * Creates the files table on the database.
    */
  def createFilesTable: Future[Unit] = {
    dtbase.run(createFilesTableAction)
  }

  /**
    * Drops the files table on the database.
    */
  def dropFilesTable: Future[Unit] = {
    dtbase.run(dropFilesTableAction)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing its fileId.
    * @param fileId Id of the file on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileId(fileId: String): Future[Boolean] = {
    dtbase.run(selectById(fileId).exists.result)
  }

  /**
    * Checks if a corresponding file row exists on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    * @return true if row exists, false if not.
    */
  def existsCorrespondingFileName(fileName: String): Future[Boolean] = {
    dtbase.run(selectByFileName(fileName).exists.result)
  }

  /**
    * Retrieves a fileId of a row on the database by providing the fileName.
    * @param fileName Name of the file given by the user on the database.
    */
  def selectFileIdFromFileName(fileName: String): Future[String] = {
    dtbase.run(selectByFileName(fileName).result.head.map(_.fileId))
  }

  /**
    * Retrieves a fileName of a row on the database by providing the fileId.
    * @param fileId Id of the file on the database.
    */
  def selectFileNameFromFileId(fileId: String): Future[String] = {
    dtbase.run(selectById(fileId).map(_.fileName).result.head)
  }

  /**
    * Method that inserts a file (row) on the files table on the database.
    * @param file FileDTO to be inserted on the database.
    */
  def insertInFilesTable(file: FileDTO): Future[Boolean] = {
    dtbase.run(insertFile(FileRow(file.fileId, file.fileName, file.uploadDate)).map(i => i == 1))

    /*exec(insertFile(FileRow(file.fileId, file.fileName, file.uploadDate)))*/

    /*fileRepo.existsCorrespondingFileName(task.fileName).flatMap {exists =>
      if(exists) taskDTOToTaskRow(task).flatMap(elem => exec(insertTask(elem)).map(i => i == 1))
      else Future.successful(false)
    }*/
  }

}
