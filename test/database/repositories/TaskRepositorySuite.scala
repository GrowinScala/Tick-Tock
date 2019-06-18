package database.repositories

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ FileDTO, TaskDTO }
import api.services.{ PeriodType, SchedulingType }
import api.utils.DateUtils._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.mappings.ExclusionMappings._
import database.mappings.SchedulingMappings._
import database.repositories.exclusion.ExclusionRepository
import database.repositories.file.FileRepository
import database.repositories.scheduling.SchedulingRepository
import database.repositories.task.TaskRepository
import org.scalatest._
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class TaskRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
  private implicit val taskRepo: TaskRepository = injector.instanceOf[TaskRepository]
  //private implicit val exclusionRepo: ExclusionRepository = injector.instanceOf[ExclusionRepository]
  //private implicit val schedulingRepo: SchedulingRepository = injector.instanceOf[SchedulingRepository]
  private val dtbase: Database = injector.instanceOf[Database]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val taskUUID1: String = UUID.randomUUID().toString
  private val taskUUID2: String = UUID.randomUUID().toString
  private val taskUUID3: String = UUID.randomUUID().toString
  private val taskUUID4: String = UUID.randomUUID().toString

  private val fileUUID1: String = UUID.randomUUID().toString
  private val fileUUID2: String = UUID.randomUUID().toString
  private val fileUUID3: String = UUID.randomUUID().toString
  private val fileUUID4: String = UUID.randomUUID().toString

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(dtbase.run(createTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(createExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(createSchedulingsTableAction), Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  override def afterEach: Unit = {
    Await.result(taskRepo.deleteAllTasks, Duration.Inf)
  }

  "DBTasksTable#drop/createTasksTable" should {
    "create and then drop the Tasks table on the database." in {
      println(Await.result(dtbase.run(MTable.getTables), Duration.Inf))
      for {
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("exclusions") && item.tail.head.name.name.equals("files") && item.tail.tail.head.name.name.equals("schedulings") && item.tail.tail.tail.head.name.name.equals("tasks") && item.size == 4))
        _ <- dtbase.run(dropTasksTableAction)
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("exclusions") && item.tail.head.name.name.equals("files") && item.tail.tail.head.name.name.equals("schedulings") && item.size == 3))
        _ <- dtbase.run(createTasksTableAction)
        result <- dtbase.run(MTable.getTables)
      } yield {
        result.head.name.name mustBe "exclusions"
        result.tail.head.name.name mustBe "files"
        result.tail.tail.head.name.name mustBe "schedulings"
        result.tail.tail.tail.head.name.name mustBe "tasks"
        result.size mustBe 4
      }
    }
  }

  "DBTasksTable#insertInTasksTable,selectAllTasks" should {
    "insert rows into the Tasks table on the database and select all rows" in {
      for {
        _ <- taskRepo.selectAllTasks.map(seq => assert(seq.isEmpty))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        resultSeq <- taskRepo.selectAllTasks
      } yield resultSeq.size mustBe 2
    }
  }

  "DBTasksTable#deleteAllTasks" should {
    "insert several rows and then delete them all from the Tasks table on the database." in {
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.selectAllTasks.map(seq => assert(seq.size == 2))
        _ <- taskRepo.deleteAllTasks
        resultSeq <- taskRepo.selectAllTasks
      } yield resultSeq.isEmpty mustBe true
    }
  }

  "DBTasksTable#taskRowToTaskDTO" should {
    "receive a taskRow and successfully transform it into its corresponding taskDTO." in {
      taskRepo.taskRowToTaskDTO(None).map {
        _ mustBe Some(TaskDTO("", "", SchedulingType.RunOnce, None, None, None, None, None, None, None, None, None))
      }

      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")

      taskRepo.taskRowToTaskDTO(Some(TaskRow("asd1", "dsa1", 2, Some(3), Some(startDate), Some(endDate)))).map {
        _.getOrElse(None) mustBe TaskDTO("asd1", "", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(3), Some(endDate))
      }
      taskRepo.taskRowToTaskDTO(Some(TaskRow("asd2", "dsa2", 4, Some(2), Some(startDate), Some(endDate)))).map {
        _.getOrElse(None) mustBe TaskDTO("asd2", "", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Weekly), Some(2), Some(endDate))
      }
      taskRepo.taskRowToTaskDTO(Some(TaskRow("asd2", "dsa2", 6, Some(1), Some(startDate), Some(endDate)))).map {
        _.getOrElse(None) mustBe TaskDTO("asd2", "", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Yearly), Some(1), Some(endDate))
      }
    }
  }

  "DBTasksTable#taskDTOToTaskRow" should {
    "receive a taskDTO and successfully transform it into its corresponding taskRow." in {
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      taskRepo.taskDTOToTaskRow(TaskDTO("asd1", "dsa1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Daily), Some(1), Some(endDate))).map {
        _ mustBe TaskRow("asd1", "", 3, Some(1), Some(startDate), Some(endDate))
      }

    }
  }

  "DBTasksTable#selectTaskByTaskId" should {
    "insert several rows and select a specific task by giving its taskId." in {
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.selectTask(taskUUID1).map(dto => assert(dto.get.fileName == "test1"))
        task <- taskRepo.selectTask(taskUUID2)
      } yield task.get.fileName mustBe "test2"
    }
  }

  "DBTasksTable#selectFileIdByTaskId" should {
    "inserts several rows and select a specific fileId from a task by giving its taskId." in {
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "unknown", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        first <- taskRepo.selectFileIdByTaskId(taskUUID1).map(fileId => assert(fileId.get == fileUUID1))
        second <- taskRepo.selectFileIdByTaskId(taskUUID3).map(fileId => assert(fileId.get == ""))
        third <- taskRepo.selectFileIdByTaskId(taskUUID4).map(fileId => assert(fileId.isEmpty))
        fileId <- taskRepo.selectFileIdByTaskId(taskUUID2)
      } yield fileId.get mustBe fileUUID2

    }
  }

  "DBTasksTable#selectTotalOccurrencesByTaskId" should {
    "inserts several rows and select the totalOccurrences from a task by giving its taskId." in {
      val date = stringToDateFormat("01-07-2019 00:00:00", "dd-MM-yyyy HH:mm:ss")
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Hourly), Some(1), Some(date)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID4, "unknown", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3)))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID1).map(totalOccurrences => assert(totalOccurrences.contains(5)))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID4).map(totalOccurrences => assert(totalOccurrences.isEmpty))
        _ <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID3).map(totalOccurrences => assert(totalOccurrences.isEmpty))
        totalOccurrences <- taskRepo.selectTotalOccurrencesByTaskId(taskUUID2)
      } yield totalOccurrences.isEmpty mustBe true
    }
  }

  "DBTasksTable#selectCurrentOccurrencesByTaskId" should {
    "insert several rows and select the currentOccurrences from a task by giving its taskId" in {
      val date = stringToDateFormat("01-07-2019 00:00:00", "dd-MM-yyyy HH:mm:ss")
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Hourly), Some(1), Some(date)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.RunOnce, Some(getCurrentDateTimestamp)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3), Some(3)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID4, "unknown", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5), Some(5)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID3).map(currentOccurrences => assert(currentOccurrences.contains(3)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID4).map(currentOccurrences => assert(currentOccurrences.isEmpty))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2).map(currentOccurrences => assert(currentOccurrences.isEmpty))
        currentOccurrences <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1)
      } yield currentOccurrences.isEmpty mustBe true
    }
  }

  "DBTasksTable#decrementCurrentOccurrencesByTaskId" should {
    "insert several rows and decrement the currentOccurrences field by 1 from a task by giving its taskId" in {
      for {
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Monthly), Some(3), None, Some(3), Some(3)))
        _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(getCurrentDateTimestamp), Some(PeriodType.Minutely), Some(2), None, Some(5), Some(5)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1).map(currentOccurrences => assert(currentOccurrences.contains(3)))
        _ <- taskRepo.decrementCurrentOccurrencesByTaskId(taskUUID1)
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID1).map(currentOccurrences => assert(currentOccurrences.contains(2)))
        _ <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2).map(currentOccurrences => assert(currentOccurrences.contains(5)))
        _ <- taskRepo.decrementCurrentOccurrencesByTaskId(taskUUID2)
        currentOccurrences <- taskRepo.selectCurrentOccurrencesByTaskId(taskUUID2)
      } yield currentOccurrences.contains(4) mustBe true
    }
  }
}
