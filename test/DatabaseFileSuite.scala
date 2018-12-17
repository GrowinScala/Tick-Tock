import database.repositories.FileRepository
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Inside}
import org.scalatestplus.play.PlaySpec
import slick.jdbc.H2Profile.api._
import database.mappings.FileMappings._
import api.services.FileService._
import api.services.TaskService._
import slick.jdbc.meta.MTable
import database.utils.DatabaseUtils._

import scala.concurrent._
import scala.concurrent.duration._

class DatabaseFileSuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach{

  val fileRepo = new FileRepository(TEST_DB)

  override def beforeEach(): Unit = {
    fileRepo.createFilesTable
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

  "DBFilesTable#insertInFilesTable" should {
    "insert rows into the Files table on the database and check if they were inserted correctly." in {
      assert(fileRepo.selectAllFiles.isEmpty)
      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      assert(fileRepo.selectAllFiles.size == 2)
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test4", "asd4", getCurrentDateTimestamp))
      assert(fileRepo.selectAllFiles.size == 4)
    }
  }

  "DBFilesTable#SelectAllFiles" should {
    "insert and select all rows from the Files table on the database." in {

      assert(fileRepo.selectAllFiles.isEmpty)
      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      assert(fileRepo.selectAllFiles.size == 1 && fileRepo.selectAllFiles.head.fileName == "test1")
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      assert(fileRepo.selectAllFiles.size == 3)
      assert(fileRepo.selectAllFiles.tail.head.fileName == "test2")
      assert(fileRepo.selectAllFiles.last.fileName == "test3")

    }
  }

  "DBFilesTable#DeleteAllFiles" should {
    "insert several rows and then delete them all from the Files table on the database." in {

      assert(fileRepo.selectAllFiles.isEmpty)
      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      assert(fileRepo.selectAllFiles.size == 3)
      fileRepo.deleteAllFiles
      assert(fileRepo.selectAllFiles.isEmpty)

    }
  }

  "DBFilesTable#existsCorrespondingFileId" should {
    "insert some rows into the Files table on the database and check if certain fileId's exist." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      assert(!fileRepo.existsCorrespondingFileId(0)) // fileId 0 shouldn't exist.
      assert(fileRepo.existsCorrespondingFileId(1))
      assert(fileRepo.existsCorrespondingFileId(2))
      assert(!fileRepo.existsCorrespondingFileId(3)) // fileId 3 shouldn't exist.
      assert(!fileRepo.existsCorrespondingFileId(4)) // fileId 4 shouldn't exist.
      assert(!fileRepo.existsCorrespondingFileId(5)) // fileId 5 shouldn't exist.
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      fileRepo.insertInFilesTable(FileRow(0, "test4", "asd4", getCurrentDateTimestamp)) // fileId should be 4
      assert(!fileRepo.existsCorrespondingFileId(0)) // fileId 0 shouldn't exist.
      assert(fileRepo.existsCorrespondingFileId(1))
      assert(fileRepo.existsCorrespondingFileId(2))
      assert(fileRepo.existsCorrespondingFileId(3))
      assert(fileRepo.existsCorrespondingFileId(4))
      assert(!fileRepo.existsCorrespondingFileId(5)) // fileId 5 shouldn't exist.

    }
  }

  "DBFilesTable#existsCorrespondingFileName" should {
    "insert some rows into the Files table on the database and check if certain fileName's exist." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      assert(!fileRepo.existsCorrespondingFileName("test0")) // "test0" shouldn't exist.
      assert(fileRepo.existsCorrespondingFileName("test1"))
      assert(fileRepo.existsCorrespondingFileName("test2"))
      assert(fileRepo.existsCorrespondingFileName("test3"))
      assert(!fileRepo.existsCorrespondingFileName("test4")) // "test4" shouldn't exist.

    }
  }

  "DBFilesTable#selectFileIdFromName" should {
    "insert some rows into the Files table on the database and retrieve fileId's by giving fileName's." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      assert(fileRepo.selectFileIdFromName("test1") == 1)
      assert(fileRepo.selectFileIdFromName("test3") != 1)
      assert(fileRepo.selectFileIdFromName("test1") != 2)
      assert(fileRepo.selectFileIdFromName("test2") == 2)
      assert(fileRepo.selectFileIdFromName("test2") != 3)
      assert(fileRepo.selectFileIdFromName("test3") == 3)

    }
  }

  "DBFilesTable#selectNameFromFileId" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving FileId's." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      assert(fileRepo.selectFileNameFromFileId(1) == "test1")
      assert(fileRepo.selectFileNameFromFileId(3) != "test1")
      assert(fileRepo.selectFileNameFromFileId(1) != "test2")
      assert(fileRepo.selectFileNameFromFileId(2) == "test2")
      assert(fileRepo.selectFileNameFromFileId(2) != "test3")
      assert(fileRepo.selectFileNameFromFileId(3) == "test3")

    }
  }

  "DBFilesTable#selectFilePathFromFileName" should {
    "insert some rows into the Files table on the database and retrieve storageName's by giving fileName's." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      assert(fileRepo.selectStorageNameFromFileName("test1") == "asd1")
      assert(fileRepo.selectStorageNameFromFileName("test3") != "asd1")
      assert(fileRepo.selectStorageNameFromFileName("test1") != "asd2")
      assert(fileRepo.selectStorageNameFromFileName("test2") == "asd2")
      assert(fileRepo.selectStorageNameFromFileName("test2") != "asd3")
      assert(fileRepo.selectStorageNameFromFileName("test3") == "asd3")

    }
  }

  "DBFilesTable#selectFileNameFromFilePath" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving storageName's." in {

      fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileRow(0, "test3", "asd3", getCurrentDateTimestamp))
      assert(fileRepo.selectFileNameFromStorageName("asd1") == "test1")
      assert(fileRepo.selectFileNameFromStorageName("asd3") != "test1")
      assert(fileRepo.selectFileNameFromStorageName("asd1") != "test2")
      assert(fileRepo.selectFileNameFromStorageName("asd2") == "test2")
      assert(fileRepo.selectFileNameFromStorageName("asd2") != "test3")
      assert(fileRepo.selectFileNameFromStorageName("asd3") == "test3")

    }
  }


}
