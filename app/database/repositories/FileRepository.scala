package database.repositories

import database.mappings.FileMappings._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

object FileRepository extends BaseRepository{

  def selectAllFiles: Future[Seq[FileRow]] = {
    exec(selectAllFromFilesTable.result)
  }

  def selectFileById(id: Int): Future[Seq[FileRow]] = {
    exec(selectById(id).result)
  }

  def deleteAllFiles: Future[Int]  = {
    exec(deleteAllFromFilesTable)
  }

  def createFilesTable: Unit = {
    exec(createFilesTableAction)
  }

  def dropFilesTable: Unit = {
    exec(dropFilesTableAction)
  }

  def existsCorrespondingFileId(fileId: Int): Boolean = {
    exec(selectById(fileId).result) != Vector()
  }

  def existsCorrespondingFileName(fileName: String): Boolean = {
    exec(selectByFileName(fileName).result) != Vector()
  }

  def selectFileIdFromName(fileName: String): Future[Int] = {
    exec(selectByFileName(fileName).map(_.fileId).result.head)
  }

  def selectNameFromFileId(fileId: Int): Future[String] = {
    exec(selectById(fileId).map(_.fileName).result.head)
  }

  def selectFilePathFromFileName(fileName: String): Future[String] = {
    exec(selectByFileName(fileName).map(_.storageName).result.head)
  }

  def selectFileNameFromFilePath(filePath: String): Future[String] = {
    exec(selectByStorageName(filePath).map(_.fileName).result.head)
  }

  def insertFilesTableAction(file: FileRow): Unit = {
    exec(insertFile(file))
  }

}
