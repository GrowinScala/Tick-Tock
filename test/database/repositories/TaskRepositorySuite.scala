package database.repositories


import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.{FileDTO, TaskDTO}
import api.services.{PeriodType, SchedulingType}
import api.utils.DateUtils._
import database.utils.DatabaseUtils._
import javax.inject.Inject
import org.scalatest._
import database.mappings.FileMappings._
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class TaskRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach{


  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
  implicit val taskRepo: TaskRepository = injector.instanceOf[TaskRepository]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val taskUUID1: String = UUID.randomUUID().toString
  val taskUUID2: String = UUID.randomUUID().toString
  val taskUUID3: String = UUID.randomUUID().toString
  val taskUUID4: String = UUID.randomUUID().toString

  val fileUUID1: String = UUID.randomUUID().toString
  val fileUUID2: String = UUID.randomUUID().toString
  val fileUUID3: String = UUID.randomUUID().toString
  val fileUUID4: String = UUID.randomUUID().toString

  //def runBlocking()

  override def beforeAll = {
    val result = for {
      _ <- fileRepo.createFilesTable
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp))
      res <- fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp))
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(taskRepo.createTasksTable, Duration.Inf)

    //validate beforeAll:
    //println(Await.result(fileRepo.exec(filesTable.length.result), Duration.Inf))
  }

  override def afterAll = {
    Await.result(taskRepo.dropTasksTable, Duration.Inf)
    Await.result(fileRepo.dropFilesTable, Duration.Inf)
  }

  override def afterEach = {
    Await.result(taskRepo.deleteAllTasks, Duration.Inf)
  }

  "DBTasksTable#drop/createTasksTable" should {
    "create and then drop the Tasks table on the database." in {
      val result = for {
        _ <- TEST_DB.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("tasks")))
        _ <- taskRepo.dropTasksTable
        _ <- TEST_DB.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files")))
        _ <- taskRepo.createTasksTable
        elem <- TEST_DB.run(MTable.getTables)
      } yield elem
      result.map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("tasks")))
    }
  }

  "DBTasksTable#insertInTasksTable,selectAllTasks" should {
    "insert rows into the Tasks table on the database and select all rows" in {
      val result = for{
        _ <- taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        resultSeq <- taskRepo.selectAllTasks
      } yield resultSeq
      result.map(seq => assert(seq.size == 2))
    }
  }

  "DBTasksTable#deleteAllTasks" should {
    "insert several rows and then delete them all from the Tasks table on the database." in {
      val result = for{
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.selectAllTasks.map(seq => assert(seq.size == 2))
        _ <- taskRepo.deleteAllTasks
        resultSeq <- taskRepo.selectAllTasks
      } yield resultSeq
      result.map(seq => assert(seq.isEmpty))
    }
  }

  "DBTasksTable#selectTaskByTaskId" should {
    "insert several rows and select a specific task by giving its taskId" in {
      val result = for{
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.selectTaskByTaskId(taskUUID1).map(dto => assert(dto.fileName == "test1"))
        task <- taskRepo.selectTaskByTaskId(taskUUID2)
      } yield task
      result.map(dto => assert(dto.fileName == "test2"))
    }
  }

  "DBTasksTable#selectFileIdByTaskId" should {
    "inserts several rows and select a specific fileId from a task by giving its taskId" in {
      val result = for{
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.selectFileIdByTaskId(taskUUID1).map(fileId => assert(fileId == fileUUID1))
        elem <- taskRepo.selectFileIdByTaskId(taskUUID2)
      } yield elem
      result.map(fileId => assert(fileId == fileUUID2))
    }
  }

  "DBTasksTable#selectTotalOccurrencesByTaskId" should {
    "inserts several rows and select the totalOccurrences from a task by giving its taskId" in {
      val date = stringToDateFormat("01-07-2019 00:00:00", "dd-MM-yyyy HH:mm:ss")
      val result = for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Hourly), Some(1), Some(date)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID4, "test4", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3)))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID1).map(totalOccurrences => assert(totalOccurrences.contains(5)))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID4).map(totalOccurrences => assert(totalOccurrences.contains(3)))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID3).map(totalOccurrences => assert(totalOccurrences.isEmpty))
        elem <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID2)
      } yield elem
      result.map(totalOccurrences => assert(totalOccurrences.isEmpty))
    }
  }

  "DBTasksTable#selectCurrentOccurrencesByTaskId" should {
    "insert several rows and select the currentOccurrences from a task by giving its taskId" in {
      val date = stringToDateFormat("01-07-2019 00:00:00", "dd-MM-yyyy HH:mm:ss")
      val result = for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Hourly), Some(1), Some(date)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3), Some(3)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID4, "test4", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5), Some(5)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID3).map(currentOccurrences => assert(currentOccurrences.contains(3)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID4).map(currentOccurrences => assert(currentOccurrences.contains(5)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2).map(currentOccurrences => assert(currentOccurrences.isEmpty))
        elem <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1)
      } yield elem
      result.map(currentOccurrences => assert(currentOccurrences.isEmpty))
    }
  }

  "DBTasksTable#decrementCurrentOccurrencesByTaskId" should {
    "insert several rows and decrement the currentOccurrences field by 1 from a task by giving its taskId" in {
      val result = for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3), Some(3)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5), Some(5)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1).map(currentOccurrences => assert(currentOccurrences.contains(3)))
        _ <- taskRepo.decrementCurrentOccurrencesByTaskId(taskUUID1)
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1).map(currentOccurrences => assert(currentOccurrences.contains(2)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2).map(currentOccurrences => assert(currentOccurrences.contains(5)))
        _ <- taskRepo.decrementCurrentOccurrencesByTaskId(taskUUID2)
        elem <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2)
      } yield elem
      result.map(currentOccurrences => assert(currentOccurrences.contains(4)))
    }
  }
}
