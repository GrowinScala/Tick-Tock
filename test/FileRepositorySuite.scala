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
      fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 2))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test4", "asd4", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 4))
    }
  }

  "DBFilesTable#SelectAllFiles" should {
    "insert and select all rows from the Files table on the database." in {
      fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 1 && seq.head.fileName.equals("test1")))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 3 && seq.last.fileName.equals("test3")))
    }
  }

  "DBFilesTable#DeleteAllFiles" should {
    "insert several rows and then delete them all from the Files table on the database." in {
      fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.selectAllFiles.map(seq => assert(seq.size == 3))
      fileRepo.deleteAllFiles
      fileRepo.selectAllFiles.map(seq => assert(seq.isEmpty))
    }
  }

  "DBFilesTable#existsCorrespondingFileId" should {
    "insert some rows into the Files table on the database and check if certain fileId's exist." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      fileRepo.existsCorrespondingFileId(0).map(result => assert(!result)) //fieldId 0 does not exist
      fileRepo.existsCorrespondingFileId(1).map(result => assert(result))
      fileRepo.existsCorrespondingFileId(2).map(result => assert(result))
      fileRepo.existsCorrespondingFileId(3).map(result => assert(!result)) //fieldId 0 does not exist
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      fileRepo.insertInFilesTable(FileDTO("test4", "asd4", getCurrentDateTimestamp)) // fileId should be 4
      fileRepo.existsCorrespondingFileId(3).map(result => assert(result))
      fileRepo.existsCorrespondingFileId(4).map(result => assert(result))
      fileRepo.existsCorrespondingFileId(5).map(result => assert(!result)) //fieldId 0 does not exist
    }
  }

  "DBFilesTable#existsCorrespondingFileName" should {
    "insert some rows into the Files table on the database and check if certain fileName's exist." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp))
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp))
      fileRepo.existsCorrespondingFileName("test0").map(result => assert(!result)) //fileName "test0" does not exist
      fileRepo.existsCorrespondingFileName("test1").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test2").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test3").map(result => assert(result))
      fileRepo.existsCorrespondingFileName("test4").map(result => assert(!result)) //fileName "test0" does not exist
    }
  }

  "DBFilesTable#selectFileIdFromName" should {
    "insert some rows into the Files table on the database and retrieve fileId's by giving fileName's." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      fileRepo.selectFileIdFromName("test1").map(result => assert(result == 1))
      fileRepo.selectFileIdFromName("test2").map(result => assert(result == 2))
      fileRepo.selectFileIdFromName("test3").map(result => assert(result == 3))
    }
  }

  "DBFilesTable#selectNameFromFileId" should {
    "insert some rows into the Files table on the database and retrieve fileName's by giving FileId's." in {
      fileRepo.insertInFilesTable(FileDTO("test1", "asd1", getCurrentDateTimestamp)) // fileId should be 1
      fileRepo.insertInFilesTable(FileDTO("test2", "asd2", getCurrentDateTimestamp)) // fileId should be 2
      fileRepo.insertInFilesTable(FileDTO("test3", "asd3", getCurrentDateTimestamp)) // fileId should be 3
      fileRepo.selectFileNameFromFileId(1).map(result => assert(result.equals("test1")))
      fileRepo.selectFileNameFromFileId(2).map(result => assert(result.equals("test2")))
      fileRepo.selectFileNameFromFileId(3).map(result => assert(result.equals("test3")))
    }
  }

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
}
