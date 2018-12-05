package database.repositories

import database.mappings.FileMappings._
import slick.jdbc.MySQLProfile.api._

object FileRepository extends Repository{

  def selectAllFiles: Seq[FileRow] = {
    exec(selectAllFromFilesTable.result)
  }

  def deleteAllFiles: Int  = {
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
    exec(selectByName(fileName).result) != Vector()
  }

  def selectFileIdFromName(fileName: String): Int = {
    exec(selectByName(fileName).map(_.fileId).result.head)
  }

  def selectNameFromFileId(fileId: Int): String = {
    exec(selectById(fileId).map(_.fileName).result.head)
  }

  def selectFilePathFromFileName(fileName: String): String = {
    exec(selectByName(fileName).map(_.filePath).result.head)
  }

  def selectFileNameFromFilePath(filePath: String): String = {
    exec(selectByPath(filePath).map(_.fileName).result.head)
  }

  def insertFilesTableAction(file: FileRow): Unit = {
    exec(insertFile(file))
  }

}
