import java.util.UUID

import api.dtos.FileDTO
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import database.repositories.slick.FileRepositoryImpl
import api.utils.DateUtils._
import database.utils.DatabaseUtils._

import scala.concurrent._

class FileRepositorySuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val fileRepo = new FileRepositoryImpl(TEST_DB)

  override def beforeEach(): Unit = {
    fileRepo.createFilesTable
    fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
  }

  override def afterEach(): Unit = {
    fileRepo.dropFilesTable
  }

  /*"DBFilesTable#Create/DropFilesTable" should {
    "create and then drop the Files table on the database." in {
      fr.createFilesTable
      assert(fr.exec(MTable.getTables).toList.head.name.name == "files")
      fr.dropFilesTable
      assert(fr.exec(MTable.getTables).toList.isEmpty)
    }
  }*/

  "DBFilesTable#SelectAllFiles,insertInFilesTable" should {
    "insert rows into the Files table and make select queries on those rows" in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      val uuid3 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 1 && seq.head.fileName.equals("test1")))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 3 && seq.last.fileName.equals("test3")))
    }
  }

  "DBFilesTable#DeleteAllFiles" should {
    "insert rows into the Files table and then delete them all from the Files table on the database." in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      val uuid3 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
      fileRepo.deleteAllFiles
      fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
    }
  }

  "DBFilesTable#existsCorrespondingFileId" should {
    "insert some rows into the Files table on the database and check if certain fileId's exist." in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.selectFileIdFromFileName("test1").map(elem =>
        fileRepo.existsCorrespondingFileId(elem).map(result => assert(result))
      )
      fileRepo.selectFileIdFromFileName("test2").map(elem =>
        fileRepo.existsCorrespondingFileId(elem).map(result => assert(result))
      )
      val uuidWrong = UUID.randomUUID().toString
      fileRepo.existsCorrespondingFileId(uuidWrong).map(result => assert(!result))
    }
  }

  "DBFilesTable#existsCorrespondingFileName" should {
    "insert some rows into the Files table on the database and check if certain fileName's exist." in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      val uuid3 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
      fileRepo.existsCorrespondingFileName("test0").map(result => assert(!result)) //fileName "test0" does not exist
      fileRepo.existsCorrespondingFileName("test1").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test2").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test3").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test4").map(result => assert(!result)) //fileName "test4" does not exist
    }
  }

  "DBFilesTable#selectFileIdFromName" should {
    "insert some rows into the Files table on the database and retrieve fileId's by giving fileName's." in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      val uuid3 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
      fileRepo.selectFileIdFromFileName("test1").map(result => assert(result == uuid1))
      fileRepo.selectFileIdFromFileName("test2").map(result => assert(result == uuid2))
      fileRepo.selectFileIdFromFileName("test3").map(result => assert(result == uuid3))
    }
  }


  "DBFilesTable#selectNameFromFileId" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving FileId's." in {
      val uuid1 = UUID.randomUUID().toString
      val uuid2 = UUID.randomUUID().toString
      val uuid3 = UUID.randomUUID().toString
      fileRepo.insertInFilesTable(FileDTO(uuid1, "test1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid2, "test2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO(uuid3, "test3", getCurrentDateTimestamp))
      fileRepo.selectFileNameFromFileId(uuid1).map(result => assert(result.equals("test1")))
      fileRepo.selectFileNameFromFileId(uuid2).map(result => assert(result.equals("test2")))
      fileRepo.selectFileNameFromFileId(uuid3).map(result => assert(result.equals("test3")))
    }
  }

  /*
  "DBFilesTable#selectFilePathFromFileName" should {
    "insert some rows into the Files table on the database and retrieve storageName's by giving fileName's." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.selectStorageNameFromFileName("test1").map(result => assert(result.equals("asd1")))
      fileRepo.selectStorageNameFromFileName("test2").map(result => assert(result.equals("asd2")))
      fileRepo.selectStorageNameFromFileName("test3").map(result => assert(result.equals("asd3")))
    }
  }

  "DBFilesTable#selectFileNameFromFilePath" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving storageName's." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.selectFileNameFromStorageName("asd1").map(result => assert(result.equals("test1")))
      fileRepo.selectFileNameFromStorageName("asd2").map(result => assert(result.equals("test2")))
      fileRepo.selectFileNameFromStorageName("asd3").map(result => assert(result.equals("test3")))
    }
  }
  */
}
