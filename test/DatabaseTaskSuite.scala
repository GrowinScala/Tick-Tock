import api.dtos.TaskDTO
import database.repositories.{FileRepository, TaskRepository}
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import database.mappings.FileMappings._
import api.services.FileService._
import database.mappings.TaskMappings.TaskRow
import database.utils.DatabaseUtils._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class DatabaseTaskSuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach{

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val fileRepo = new FileRepository(TEST_DB)
  val taskRepo = new TaskRepository(TEST_DB)

  override def beforeAll() = {
    fileRepo.createFilesTable
    fileRepo.insertInFilesTable(FileRow(0, "test1", "asd1", getCurrentDateTimestamp))
    fileRepo.insertInFilesTable(FileRow(0, "test2", "asd2", getCurrentDateTimestamp))
  }

  override def beforeEach() = {
    taskRepo.createTasksTable
  }

  override def afterAll() = {
    fileRepo.dropFilesTable
  }

  override def afterEach() = {
    taskRepo.dropTasksTable
  }

  "DBTasksTable#insertInTasksTable" should {
    "insert rows into the Tasks table on the database and check if they were inserted correctly." in {
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 3, getCurrentDateTimestamp)) // this one shouldn't insert.
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test0")) // this one shouldn't insert.
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 3))
    }
  }

  "DBTasksTable#selectAllTasks" should {
    "insert and select all rows from the Tasks table on the database." in {
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 2))
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 4 && seq.last.fileId ==2))
    }
  }

  "DBTasksTable#deleteAllTasks" should {
    "insert several rows and then delete them all from the Tasks table on the database." in {
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      taskRepo.selectAllTasks.map(seq => assert(seq.size == 4))
      taskRepo.deleteAllTasks
      taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
    }
  }
}
