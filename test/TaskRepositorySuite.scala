
import java.util.UUID

import api.dtos.{FileDTO, TaskDTO}
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import api.utils.DateUtils._
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import database.utils.DatabaseUtils._

class TaskRepositorySuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach{

  val fileRepo = new FileRepositoryImpl(TEST_DB)
  val taskRepo = new TaskRepositoryImpl(TEST_DB)

  override def beforeAll = {
    fileRepo.createFilesTable
    fileRepo.insertInFilesTable(FileDTO(UUID.randomUUID().toString, "test1", getCurrentDateTimestamp))
    fileRepo.insertInFilesTable(FileDTO(UUID.randomUUID().toString, "test2", getCurrentDateTimestamp))
  }

  override def beforeEach = {
    taskRepo.createTasksTable
  }

  override def afterAll = {
    fileRepo.dropFilesTable
  }

  override def afterEach = {
    taskRepo.dropTasksTable
  }

  /*"DBTasksTable#insertInTasksTable,selectAllTasks" should {
    "insert rows into the Tasks table on the database and select all rows" in {
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
      taskRepo.insertInTasksTable(TaskDTO("asd1", stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), "test1"))
      taskRepo.insertInTasksTable(TaskDTO("asd2", stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), "test2"))
      taskRepo.insertInTasksTable(TaskDTO("asd3", stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), "test3")) // this one shouldn't insert.
      taskRepo.insertInTasksTable(TaskDTO("asd4", stringToDateFormat("2030-04-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), "test4")) // this one shouldn't insert.
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 2)) // 2 of the 4 insert attempts shouldn't insert. There should be 2 rows.
    }
  }

  "DBTasksTable#deleteAllTasks" should {
    "insert several rows and then delete them all from the Tasks table on the database." in {
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
      taskRepo.insertInTasksTable(TaskDTO("asd1", getCurrentDateTimestamp, "test1"))
      taskRepo.insertInTasksTable(TaskDTO("asd2", getCurrentDateTimestamp, "test2"))
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 2))
      taskRepo.deleteAllTasks
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
    }
  }*/
}
