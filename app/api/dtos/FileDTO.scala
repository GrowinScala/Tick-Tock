package api.dtos

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Data transfer object for the file on the service side.
 * @param fileId Name of the file given by the user of the service.
 * @param fileName Name of the .jar file in the file storage.
 * @param uploadDate Date of when the file was uploaded.
 */
case class FileDTO(
  fileId: String,
  fileName: String,
  uploadDate: Date)

/**
 * Companion object for the FileDTO
 */
object FileDTO {

  /**
   * Implicit that defines how a FileDTO is written to a JSON format.
   */
  implicit val fileReads: Reads[FileDTO] = (
    (JsPath \ "fileId").read[String] and
    (JsPath \ "fileName").read[String] and
    (JsPath \ "uploadDate").read[Date])(FileDTO.apply _)

  implicit val fileWrites = new Writes[FileDTO] {
    def writes(file: FileDTO) =
      Json.obj("fileId" -> file.fileId, "fileName" -> file.fileName, "uploadDate" -> file.uploadDate.toString)
  }
}