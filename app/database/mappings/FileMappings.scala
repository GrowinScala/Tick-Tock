package database.mappings

import java.sql.Timestamp
import java.util.Date

import slick.driver.MySQLDriver.api._

object FileMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------
  case class FileRow(
                   fileId: Int,
                   fileName: String,
                   filePath: String,
                   uploadDate: Date
                 )

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class FilesTable(tag: Tag) extends Table[FileRow](tag, "files"){
    def fileId = column[Int]("FileId", O.PrimaryKey, O.AutoInc)
    def fileName = column[String]("fileName", O.Unique, O.Length(30))
    def filePath = column[String]("filePath", O.Length(100))
    def uploadDate = column[Date]("uploadDate")

    def * = (fileId, fileName, filePath, uploadDate) <> (FileRow.tupled, FileRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val columnType: BaseColumnType[Date] =
    MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)

  private def dateToTimestamp(date: Date): Timestamp =
    new Timestamp(date.getTime)

  private def timestampToDate(timestamp: Timestamp): Date =
    new Date(timestamp.getTime)

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val filesTable = TableQuery[FilesTable]
  val createFilesTableAction = filesTable.schema.create
  val dropFilesTableAction = filesTable.schema.drop
  val deleteAllFromFilesTable = filesTable.delete
  val selectAllFromFilesTable = filesTable

  def selectById(id: Int): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.fileId === id)
  }
  def selectByName(name: String): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.fileName === name)
  }
  def selectByPath(path: String): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.filePath === path)
  }
  def insertFile(file: FileRow) = {
    filesTable += file
  }
  def updateById(id: Int, file: FileRow)= {
    filesTable.filter(_.fileId === id).update(file)
  }
  def updateByName(name: String, file: FileRow) = {
    filesTable.filter(_.fileName === name).update(file)
  }
  def updateByPath(path: String, file: FileRow) = {
    filesTable.filter(_.filePath === path).update(file)
  }
  def deleteById(id: Int) = {
    filesTable.filter(_.fileId === id).delete
  }
  def deleteByName(name: String) = {
    filesTable.filter(_.fileName === name).delete
  }
  def deleteByPath(path: String) = {
    filesTable.filter(_.filePath === path).delete
  }

}
