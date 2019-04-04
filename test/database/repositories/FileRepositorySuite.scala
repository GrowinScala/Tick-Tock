package database.repositories

import java.util.UUID

import api.dtos.FileDTO
import api.utils.DateUtils._
import org.scalatest.{ AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.meta.MTable
import slick.jdbc.MySQLProfile.api._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._

import scala.concurrent._
import scala.concurrent.duration._

class FileRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach {

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
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(dtbase.run(createTasksTableAction), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  override def afterEach(): Unit = {
    Await.result(fileRepo.deleteAllFiles, Duration.Inf)
  }

  "DBFilesTable#create/dropFilesTable" should {
    "create and then drop the Files table on the database." in {
      dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files")))
      Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
      dtbase.run(MTable.getTables).map(item => assert(item.isEmpty))
      Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
      dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files")))
    }
  }

  "DBFilesTable#SelectAllFiles,insertInFilesTable" should {
    "insert rows into the Files table and make select queries on those rows" in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        resultSeq <- fileRepo.selectAllFiles
      } yield resultSeq
      result.map(seq => assert(seq.size == 3 && seq.last.fileName.equals("test3")))
    }
  }

  "DBFilesTable#SelectFileById" should {
    "insert rows into the Files table and select a specific row by giving its fileId" in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileById(uuid1).map(file => assert(file.get.fileName.equals("test1")))
        _ <- fileRepo.selectFileById(uuid3).map(file => assert(file.get.fileName.equals("test3")))
        elem <- fileRepo.selectFileById(uuid2)
      } yield elem
      result.map(file => assert(file.get.fileName.equals("test2")))
    }
  }

  "DBFilesTable#DeleteAllFiles" should {
    "insert rows into the Files table and then delete them all from the Files table on the database." in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.deleteAllFiles
        elem <- fileRepo.selectAllFiles
      } yield elem
      result.map(seq => assert(seq.isEmpty))
    }
  }

  "DBFilesTable#DeleteFileById" should {
    "insert rows into the Files table and then delete a specific row by giving its fileId" in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectAllFiles.map(seq => assert(seq.size == 3 && seq.head.fileName.equals("test1")))
        _ <- fileRepo.deleteFileById(uuid2)
        _ <- fileRepo.selectAllFiles.map(seq => assert(seq.size == 2 && seq.tail.head.fileName.equals("test3")))
        _ <- fileRepo.deleteFileById(uuid1)
        elem <- fileRepo.selectAllFiles
      } yield elem
      result.map(seq => assert(seq.size == 1 && seq.head.fileName.equals("test3")))
    }
  }

  "DBFilesTable#existsCorrespondingFileId" should {
    "insert some rows into the Files table on the database and check if certain fileId's exist." in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        elem <- fileRepo.selectFileIdFromFileName("test1")
      } yield elem
      result.map(fileId => Await.result(fileRepo.existsCorrespondingFileId(fileId).map(result => assert(result)), Duration.Inf))
      fileRepo.selectFileIdFromFileName("test2").map(fileId =>
        Await.result(fileRepo.existsCorrespondingFileId(fileId).map(result => assert(result)), Duration.Inf))
      val uuidWrong = UUID.randomUUID().toString
      Await.result(fileRepo.existsCorrespondingFileId(uuidWrong).map(result => assert(!result)), Duration.Inf)
    }
  }

  "DBFilesTable#existsCorrespondingFileName" should {
    "insert some rows into the Files table on the database and check if certain fileName's exist." in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.existsCorrespondingFileName("test0").map(result => assert(!result)) //fileName "test0" does not exist
        _ <- fileRepo.existsCorrespondingFileName("test1").map(result => assert(result))
        _ <- fileRepo.existsCorrespondingFileName("test2").map(result => assert(result))
        _ <- fileRepo.existsCorrespondingFileName("test3").map(result => assert(result))
        elem <- fileRepo.existsCorrespondingFileName("test4") //fileName "test4" does not exist
      } yield elem
      result.map(result => assert(!result))
    }
  }

  "DBFilesTable#selectFileIdFromFileName" should {
    "insert some rows into the Files table on the database and retrieve fileId's by giving fileName's." in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileIdFromFileName("test1").map(result => assert(result == uuid1))
        _ <- fileRepo.selectFileIdFromFileName("test2").map(result => assert(result == uuid2))
        elem <- fileRepo.selectFileIdFromFileName("test3")
      } yield elem
      result.map(result => assert(result == uuid3))
    }
  }

  "DBFilesTable#selectFileNameFromFileId" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving FileId's." in {
      val result = for {
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
        _ <- fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
        _ <- fileRepo.selectFileNameFromFileId(uuid1).map(result => assert(result.equals("test1")))
        _ <- fileRepo.selectFileNameFromFileId(uuid2).map(result => assert(result.equals("test2")))
        elem <- fileRepo.selectFileNameFromFileId(uuid3)
      } yield elem
      result.map(result => assert(result.equals("test3")))
    }
  }
}
