package database.mappings

import java.util.UUID

import api.utils.DateUtils._
import database.mappings.FileMappings._
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class FileMappingsSuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach {

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  val dtbase: Database = appBuilder.injector.instanceOf[Database]

  val uuid1 = UUID.randomUUID().toString
  val uuid2 = UUID.randomUUID().toString
  val uuid3 = UUID.randomUUID().toString
  val uuid4 = UUID.randomUUID().toString
  val uuid5 = UUID.randomUUID().toString

  override def beforeAll = {
    val result = for {
      _ <- dtbase.run(createFilesTableAction)
      _ <- dtbase.run(insertFile(FileRow(uuid1, "test1", stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))
      _ <- dtbase.run(insertFile(FileRow(uuid2, "test2", stringToDateFormat("2020-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss"))))
      _ <- dtbase.run(insertFile(FileRow(uuid3, "test3", stringToDateFormat("2020-01-03 00:00:00", "yyyy-MM-dd HH:mm:ss"))))
      res <- dtbase.run(insertFile(FileRow(uuid4, "test4", stringToDateFormat("2020-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss"))))
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(dtbase.run(selectAllFromFilesTable.result), Duration.Inf).size mustBe 4

  }

  override def afterAll = {
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  "FileMappings#selectById" should {
    "return the correct FileRow when given an existing fileId." in {
      val result1 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "FileRow(" + uuid1 + ",test1,Wed Jan 01 00:00:00 GMT 2020)"
      val result2 = Await.result(dtbase.run(getFileByFileId(uuid2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "FileRow(" + uuid2 + ",test2,Thu Jan 02 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#selectByFileName" should {
    "return the correct FileRow when given an existing fileName." in {
      val result1 = Await.result(dtbase.run(getFileByFileName("test3").result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "FileRow(" + uuid3 + ",test3,Fri Jan 03 00:00:00 GMT 2020)"
      val result2 = Await.result(dtbase.run(getFileByFileName("test4").result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "FileRow(" + uuid4 + ",test4,Sat Jan 04 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#selectByUploadDate" should {
    "return the correct FileRow when given an existing uploadDate." in {
      val result1 = Await.result(dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "FileRow(" + uuid1 + ",test1,Wed Jan 01 00:00:00 GMT 2020)"
      val result2 = Await.result(dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "FileRow(" + uuid2 + ",test2,Thu Jan 02 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#insertFile" should {
    "insert the given FileRow." in {
      val insertResult = Await.result(dtbase.run(insertFile(FileRow(uuid5, "test5", stringToDateFormat("2020-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      insertResult mustBe 1
      val selectResult = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult.head.toString mustBe "FileRow(" + uuid5 + ",test5,Sun Jan 05 00:00:00 GMT 2020)"
      Await.result(dtbase.run(deleteFileByFileId(uuid5)), Duration.Inf)
    }
  }

  "FileMappings#updateFileByFileId" should {
    "update a FileRow by giving the corresponding fileId." in {
      val updateResult1 = Await.result(dtbase.run(updateFileByFileId(uuid1, FileRow(uuid5, "test5", stringToDateFormat("2020-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult1.head.toString mustBe "FileRow(" + uuid5 + ",test5,Sun Jan 05 00:00:00 GMT 2020)"
      val selectResult2 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateFileByFileId(uuid5, FileRow(uuid1, "test1", stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult4.head.toString mustBe "FileRow(" + uuid1 + ",test1,Wed Jan 01 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#updateFileByFileName" should {
    "update a FileRow by giving the corresponding fileName." in {
      val updateResult1 = Await.result(dtbase.run(updateFileByFileName("test1", FileRow(uuid5, "test5", stringToDateFormat("2020-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult1.head.toString mustBe "FileRow(" + uuid5 + ",test5,Sun Jan 05 00:00:00 GMT 2020)"
      val selectResult2 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateFileByFileName("test5", FileRow(uuid1, "test1", stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult4.head.toString mustBe "FileRow(" + uuid1 + ",test1,Wed Jan 01 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#updateFileByUploadDate" should {
    "update a FileRow by giving the corresponding uploadDate." in {
      val updateResult1 = Await.result(dtbase.run(updateFileByUploadDate(stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), FileRow(uuid5, "test5", stringToDateFormat("2020-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult1.head.toString mustBe "FileRow(" + uuid5 + ",test5,Sun Jan 05 00:00:00 GMT 2020)"
      val selectResult2 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateFileByUploadDate(stringToDateFormat("2020-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss"), FileRow(uuid1, "test1", stringToDateFormat("2020-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(getFileByFileId(uuid5).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(getFileByFileId(uuid1).result), Duration.Inf)
      selectResult4.head.toString mustBe "FileRow(" + uuid1 + ",test1,Wed Jan 01 00:00:00 GMT 2020)"
    }
  }

  "FileMappings#deleteFileByFileId" should {
    "delete a FileRow by giving the corresponding fileId." in {
      val fileId = uuid1
      val selectResult1 = Await.result(dtbase.run(getFileByFileId(fileId).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteFileByFileId(fileId)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getFileByFileId(fileId).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
    }
  }

  "FileMappings#deleteFileByFileName" should {
    "delete a FileRow by giving the corresponding fileName." in {
      val fileName = "test2"
      val selectResult1 = Await.result(dtbase.run(getFileByFileName(fileName).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteFileByFileName(fileName)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getFileByFileName(fileName).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
    }
  }

  "FileMappings#deleteFileByUploadDate" should {
    "delete a FileRow by giving the corresponding uploadDate" in {
      val uploadDate = stringToDateFormat("2020-01-03 00:00:00", "yyyy-MM-dd HH:mm:ss")
      val selectResult1 = Await.result(dtbase.run(getFileByUploadDate(uploadDate).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteFileByUploadDate(uploadDate)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(getFileByUploadDate(uploadDate).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
    }
  }
}