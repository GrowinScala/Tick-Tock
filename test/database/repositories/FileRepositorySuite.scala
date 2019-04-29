package database.repositories

import java.util.UUID

import api.dtos.FileDTO
import api.utils.DateUtils._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers }
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.meta.MTable
import slick.jdbc.MySQLProfile.api._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.repositories.file.FileRepository

import scala.concurrent._
import scala.concurrent.duration._

class FileRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
  private val dtbase: Database = injector.instanceOf[Database]

  private val uuid1 = UUID.randomUUID().toString
  private val uuid2 = UUID.randomUUID().toString
  private val uuid3 = UUID.randomUUID().toString
  private val uuid4 = UUID.randomUUID().toString

  override def beforeAll(): Unit = {
    for {
      _ <- dtbase.run(createFilesTableAction)
      result <- dtbase.run(createTasksTableAction)
    } yield result
  }

  override def afterAll(): Unit = {
    for {
      _ <- dtbase.run(dropTasksTableAction)
      result <- dtbase.run(dropFilesTableAction)
    } yield result
  }

  override def afterEach(): Unit = {
    for(result <- fileRepo.deleteAllFiles) yield result
  }

  "DBFilesTable#create/dropFilesTable" should {
    "create and then drop the Files table on the database." in {
      for{
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files")))
        _ <- dtbase.run(dropFilesTableAction)
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.isEmpty))
        _ <- dtbase.run(createFilesTableAction)
        result <- dtbase.run(MTable.getTables)
      } yield result.head.name.name mustBe "files"
    }
  }

  "DBFilesTable#SelectAllFiles,insertInFilesTable" should {
    "insert rows into the Files table and make select queries on those rows" in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        resultSeq <- fileRepo.selectAllFiles
      } yield {
        resultSeq.size mustBe 3
        resultSeq.last.fileName mustBe "test3"
      }
    }
  }

  "DBFilesTable#SelectFileById" should {
    "insert rows into the Files table and select a specific row by giving its fileId" in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileById(uuid1).map(file => assert(file.get.fileName.equals("test1")))
        _ <- fileRepo.selectFileById(uuid3).map(file => assert(file.get.fileName.equals("test3")))
        file <- fileRepo.selectFileById(uuid2)
      } yield file.get.fileName mustBe "test2"
    }
  }

  "DBFilesTable#DeleteAllFiles" should {
    "insert rows into the Files table and then delete them all from the Files table on the database." in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.deleteAllFiles
        result <- fileRepo.selectAllFiles
      } yield result.isEmpty mustBe true
    }
  }

  "DBFilesTable#DeleteFileById" should {
    "insert rows into the Files table and then delete a specific row by giving its fileId" in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectAllFiles.map(seq => assert(seq.size == 3 && seq.head.fileName.equals("test1")))
        _ <- fileRepo.deleteFileById(uuid2)
        _ <- fileRepo.selectAllFiles.map(seq => assert(seq.size == 2 && seq.tail.head.fileName.equals("test3")))
        _ <- fileRepo.deleteFileById(uuid1)
        result <- fileRepo.selectAllFiles
      } yield {
        result.size mustBe 1
        result.head.fileName mustBe "test3"
      }
    }
  }

  "DBFilesTable#existsCorrespondingFileId" should {
    "insert some rows into the Files table on the database and check if certain fileId's exist." in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        fileId1 <- fileRepo.selectFileIdFromFileName("test1")
        result1 <- fileRepo.existsCorrespondingFileId(fileId1).map(result => assert(result))
        fileId2 <- fileRepo.selectFileIdFromFileName("test2")
        result2 <- fileRepo.existsCorrespondingFileId(fileId2).map(result => assert(result))
        uuidWrong = UUID.randomUUID.toString
        result3 <- fileRepo.existsCorrespondingFileId(uuidWrong)
      } yield {
        result1 mustBe true
        result2 mustBe true
        result3 mustBe false
      }
    }
  }

  "DBFilesTable#existsCorrespondingFileName" should {
    "insert some rows into the Files table on the database and check if certain fileName's exist." in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.existsCorrespondingFileName("test0").map(result => assert(!result)) //fileName "test0" does not exist
        _ <- fileRepo.existsCorrespondingFileName("test1").map(result => assert(result))
        _ <- fileRepo.existsCorrespondingFileName("test2").map(result => assert(result))
        _ <- fileRepo.existsCorrespondingFileName("test3").map(result => assert(result))
        result <- fileRepo.existsCorrespondingFileName("test4") //fileName "test4" does not exist
      } yield result mustBe false
    }
  }

  "DBFilesTable#selectFileIdFromFileName" should {
    "insert some rows into the Files table on the database and retrieve fileId's by giving fileName's." in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileIdFromFileName("test1").map(result => assert(result == uuid1))
        _ <- fileRepo.selectFileIdFromFileName("test2").map(result => assert(result == uuid2))
        result <- fileRepo.selectFileIdFromFileName("test3")
      } yield result mustBe uuid3
    }
  }

  "DBFilesTable#selectFileNameFromFileId" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving FileId's." in {
      for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileNameFromFileId(uuid1).map(result => assert(result.equals("test1")))
        _ <- fileRepo.selectFileNameFromFileId(uuid2).map(result => assert(result.equals("test2")))
        result <- fileRepo.selectFileNameFromFileId(uuid3)
      } yield result mustBe "test3"
    }
  }
}
