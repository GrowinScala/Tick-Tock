import api.dtos.TaskDTO
import database.repositories.{FileRepository, TaskRepository}
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import slick.jdbc.H2Profile.api._
import database.mappings.FileMappings._
import api.services.TaskService._
import database.mappings.TaskMappings.TaskRow

class DatabaseTaskSuite extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach{

  val db = Database.forURL("jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
    driver="org.h2.Driver")

  val fileRepo = new FileRepository(db)
  val taskRepo = new TaskRepository(db)

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
      assert(taskRepo.selectAllTasks.isEmpty)
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 3, getCurrentDateTimestamp)) // this one shouldn't insert.
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test0")) // this one shouldn't insert.
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      assert(taskRepo.selectAllTasks.size == 4) // 2 of the 6 insert attempts shouldn't insert. There should be 4 rows.
    }
  }

  "DBTasksTable#selectAllTasks" should {
    "insert and select all rows from the Tasks table on the database." in {
      assert(taskRepo.selectAllTasks.isEmpty)
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      assert(taskRepo.selectAllTasks.size == 2)
      assert(taskRepo.selectAllTasks.tail.head.fileId == 1)
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      assert(taskRepo.selectAllTasks.size == 4)
      assert(taskRepo.selectAllTasks.last.fileId == 2)
    }
  }

  "DBTasksTable#deleteAllTasks" should {
    "insert several rows and then delete them all from the Tasks table on the database." in {
      assert(taskRepo.selectAllTasks.isEmpty)
      taskRepo.insertInTasksTable(TaskRow(0, 1, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskRow(0, 2, getCurrentDateTimestamp))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test1"))
      taskRepo.insertInTasksTable(TaskDTO(getCurrentDateTimestamp, "test2"))
      assert(taskRepo.selectAllTasks.size == 4)
      taskRepo.deleteAllTasks
      assert(taskRepo.selectAllTasks.isEmpty)
    }
  }
}
