package database.mappings

import java.sql.Timestamp
import java.util.{Date, UUID}

import play.api.libs.json.{Json, OFormat}
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
                   uploadDate: Date
                 )

  implicit val fileRowFormat: OFormat[FileRow] = Json.format[FileRow]

  //---------------------------------------------------------
  //# TABLE MAPPINGS
  //---------------------------------------------------------
  class FilesTable(tag: Tag) extends Table[FileRow](tag, "files"){
    def fileId = column[String]("fileId", O.PrimaryKey, O.Length(36))
    def fileName = column[String]("fileName", O.Unique, O.Length(50))
    def uploadDate = column[Date]("uploadDate")

    def * = (fileId, fileName, uploadDate) <> (FileRow.tupled, FileRow.unapply)
  }

  //---------------------------------------------------------
  //# TYPE MAPPINGS
  //---------------------------------------------------------
  implicit val dateColumnType: BaseColumnType[Date] = MappedColumnType.base[Date, Timestamp](dateToTimestamp, timestampToDate)
  private def dateToTimestamp(date: Date): Timestamp = new Timestamp(date.getTime)
  private def timestampToDate(timestamp: Timestamp): Date = new Date(timestamp.getTime)

  /*
  implicit val uuidColumnType: BaseColumnType[UUID] = MappedColumnType.base[UUID, String](uuidToString, stringToUUID)
  private def uuidToString(uuid: UUID): String = uuid.toString
  private def stringToUUID(string: String): UUID = UUID.fromString(string)
  */

  //---------------------------------------------------------
  //# QUERY EXTENSIONS
  //---------------------------------------------------------
  lazy val filesTable = TableQuery[FilesTable]
  val createFilesTableAction = filesTable.schema.create
  /*val createFilesTableActionSQL = sqlu"""
    CREATE TABLE `ticktock`.`files` (
    `fileId` VARCHAR(36) NOT NULL,
    `fileName` VARCHAR(50) NOT NULL,
    `uploadDate` TIMESTAMP NOT NULL,
    PRIMARY KEY (`fileId`),
    UNIQUE INDEX `fileName_UNIQUE` (`fileName` ASC) VISIBLE);""".*/
  val dropFilesTableAction = filesTable.schema.drop
  //val dropFilesTableActionSQL = sqlu"DROP TABLE `ticktock`.`files`;"
  val selectAllFromFilesTable = filesTable
  val deleteAllFromFilesTable = filesTable.delete

  def selectById(id: String) = {
    filesTable.filter(_.fileId === id)
  }
  def selectByFileName(name: String)  = {
    filesTable.filter(_.fileName === name)
  }
  def insertFile(file: FileRow) = {
    filesTable += file
  }
  def updateById(id: String, file: FileRow)= {
    filesTable.filter(_.fileId === id).update(file)
  }
  def updateByFileName(name: String, file: FileRow) = {
    filesTable.filter(_.fileName === name).update(file)
  }
  def deleteById(id: String) = {
    filesTable.filter(_.fileId === id).delete
  }
  def deleteByFileName(name: String) = {
    filesTable.filter(_.fileName === name).delete
  }

}
