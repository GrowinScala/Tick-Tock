package database.mappings

import java.util.UUID

import api.utils.DateUtils._
import database.mappings.FileMappings._
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class FileMappingsSuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val uuid1 = UUID.randomUUID().toString
  private val uuid2 = UUID.randomUUID().toString
  private val uuid3 = UUID.randomUUID().toString
  private val uuid4 = UUID.randomUUID().toString
  private val uuid5 = UUID.randomUUID().toString

  private val test1 = "test1"
  private val test2 = "test2"
  private val test3 = "test3"
  private val test4 = "test4"
  private val test5 = "test5"

  private val timeFormat = "yyyy-MM-dd HH:mm:ss"

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(dtbase.run(insertFile(FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat)))), Duration.Inf)
    Await.result(dtbase.run(insertFile(FileRow(uuid2, test2, stringToDateFormat("2020-01-02 00:00:00", timeFormat)))), Duration.Inf)
    Await.result(dtbase.run(insertFile(FileRow(uuid3, test3, stringToDateFormat("2020-01-03 00:00:00", timeFormat)))), Duration.Inf)
    Await.result(dtbase.run(insertFile(FileRow(uuid4, test4, stringToDateFormat("2020-01-04 00:00:00", timeFormat)))), Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  "FileMappings#selectById" should {
    "return the correct FileRow when given an existing fileId." in {

      for {
        result1 <- dtbase.run(getFileByFileId(uuid1).result)
        result2 <- dtbase.run(getFileByFileId(uuid2).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat))
        result2.size mustBe 1
        result2.head mustBe FileRow(uuid2, test2, stringToDateFormat("2020-01-02 00:00:00", timeFormat))
      }
    }
  }

  "FileMappings#selectByFileName" should {
    "return the correct FileRow when given an existing fileName." in {

      for {
        result1 <- dtbase.run(getFileByFileName(test3).result)
        result2 <- dtbase.run(getFileByFileName(test4).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe FileRow(uuid3, test3, stringToDateFormat("2020-01-03 00:00:00", timeFormat))
        result2.size mustBe 1
        result2.head mustBe FileRow(uuid4, test4, stringToDateFormat("2020-01-04 00:00:00", timeFormat))
      }
    }
  }

  "FileMappings#selectByUploadDate" should {
    "return the correct FileRow when given an existing uploadDate." in {

      for {
        result1 <- dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-01 00:00:00", timeFormat)).result)
        result2 <- dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-02 00:00:00", timeFormat)).result)
      } yield {
        result1.size mustBe 1
        result1.head mustBe FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat))
        result2.size mustBe 1
        result2.head mustBe FileRow(uuid2, test2, stringToDateFormat("2020-01-02 00:00:00", timeFormat))
      }
    }
  }
  "FileMappings#insertFile" should {
    "insert the given FileRow." in {

      for {
        result1 <- dtbase.run(insertFile(FileRow(uuid5, test5, stringToDateFormat("2020-01-05 00:00:00", timeFormat))))
        result2 <- dtbase.run(getFileByFileId(uuid5).result)
        _ <- dtbase.run(deleteFileByFileId(uuid5))
      } yield {
        result1 mustBe 1
        result2.head mustBe FileRow(uuid5, test5, stringToDateFormat("2020-01-05 00:00:00", timeFormat))
      }
    }
  }

  "FileMappings#updateFileByFileName" should {
    "update a FileRow by giving the corresponding fileName." in {

      for {
        result1 <- dtbase.run(updateFileByFileName(uuid1, test5))
        result2 <- dtbase.run(getFileByFileId(uuid1).result)
        result3 <- dtbase.run(updateFileByFileName(uuid1, test1))
        result4 <- dtbase.run(getFileByFileId(uuid1).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe FileRow(uuid1, test5, stringToDateFormat("2020-01-01 00:00:00", timeFormat))
        result3 mustBe 1
        result4.head mustBe FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat))
      }
    }
  }

  "FileMappings#updateFileByUploadDate" should {
    "update a FileRow by giving the corresponding uploadDate." in {

      for {
        result1 <- dtbase.run(updateFileByUploadDate(uuid1, stringToDateFormat("2020-01-05 00:00:00", timeFormat)))
        result2 <- dtbase.run(getFileByFileId(uuid1).result)
        result3 <- dtbase.run(updateFileByUploadDate(uuid1, stringToDateFormat("2020-01-01 00:00:00", timeFormat)))
        result4 <- dtbase.run(getFileByFileId(uuid1).result)
      } yield {
        result1 mustBe 1
        result2.head mustBe FileRow(uuid1, test1, stringToDateFormat("2020-01-05 00:00:00", timeFormat))
        result3 mustBe 1
        result4.head mustBe FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat))
      }
    }
  }

  "FileMappings#deleteFileByFileId" should {
    "delete a FileRow by giving the corresponding fileId." in {

      for {
        result1 <- dtbase.run(getFileByFileId(uuid1).result)
        result2 <- dtbase.run(deleteFileByFileId(uuid1))
        result3 <- dtbase.run(getFileByFileId(uuid1).result)
        result4 <- dtbase.run(insertFile(FileRow(uuid1, test1, stringToDateFormat("2020-01-01 00:00:00", timeFormat))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "FileMappings#deleteFileByFileName" should {
    "delete a FileRow by giving the corresponding fileName." in {

      for {
        result1 <- dtbase.run(getFileByFileName(test2).result)
        result2 <- dtbase.run(deleteFileByFileName(test2))
        result3 <- dtbase.run(getFileByFileName(test2).result)
        result4 <- dtbase.run(insertFile(FileRow(uuid2, test2, stringToDateFormat("2020-01-02 00:00:00", timeFormat))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

  "FileMappings#deleteFileByUploadDate" should {
    "delete a FileRow by giving the corresponding uploadDate" in {

      for {
        result1 <- dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-03 00:00:00", timeFormat)).result)
        result2 <- dtbase.run(deleteFileByUploadDate(stringToDateFormat("2020-01-03 00:00:00", timeFormat)))
        result3 <- dtbase.run(getFileByUploadDate(stringToDateFormat("2020-01-03 00:00:00", timeFormat)).result)
        result4 <- dtbase.run(insertFile(FileRow(uuid3, test3, stringToDateFormat("2020-01-03 00:00:00", timeFormat))))
      } yield {
        result1.nonEmpty mustBe true
        result2 mustBe 1
        result3.isEmpty mustBe true
        result4 mustBe 1
      }
    }
  }

}