package database.mappings

import java.sql.Timestamp
import java.util.Date

import play.api.libs.json.{Json, OFormat}
import slick.dbio.Effect
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

/**
 * Object that contains the representation of the File table Row,
 * the slick mappings for the table
 * and query extensions for that table.
 */
object FileMappings {

  //---------------------------------------------------------
  //# ROW REPRESENTATION
  //---------------------------------------------------------
  case class FileRow(
    fileId: String,
    fileName: String,
    uploadDate: Date)

  implicit val fileRowFormat: OFormat[FileRow] = Json.format[FileRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class FilesTable(tag: Tag) extends Table[FileRow](tag, "files") {
    def fileId = column[String]("fileId", O.Length(36))
    def fileName = column[String]("fileName", O.Length(50))
    def uploadDate = column[Date]("uploadDate")

    def * = (fileId, fileName, uploadDate) <> (FileRow.tupled, FileRow.unapply)
  }

  //---------------------------------------------------------
  //# FILES TABLE TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val filesTable = TableQuery[FilesTable]
  val createFilesTableAction = filesTable.schema.create
  val dropFilesTableAction = filesTable.schema.drop
  val selectAllFromFilesTable = filesTable
  val deleteAllFromFilesTable = filesTable.delete

  def getFileByFileId(fileId: String): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.fileId === fileId)
  }

  def getFileByFileName(fileName: String): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.fileName === fileName)
  }

  def getFileByUploadDate(uploadDate: Date): Query[FilesTable, FileRow, Seq] = {
    filesTable.filter(_.uploadDate === uploadDate)
  }

  def insertFile(file: FileRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    filesTable += file
  }

  def updateFileByFileId(fileId: String, file: FileRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByFileId(fileId).update(file)
  }

  def updateFileByFileName(fileName: String, file: FileRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByFileName(fileName).update(file)
  }

  def updateFileByUploadDate(uploadDate: Date, file: FileRow): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByUploadDate(uploadDate).update(file)
  }

  def deleteFileByFileId(fileId: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByFileId(fileId).delete
  }

  def deleteFileByFileName(fileName: String): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByFileName(fileName).delete
  }

  def deleteFileByUploadDate(uploadDate: Date): MySQLProfile.ProfileAction[Int, NoStream, Effect.Write] = {
    getFileByUploadDate(uploadDate).delete
  }
}
