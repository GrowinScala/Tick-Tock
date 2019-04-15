package database.repositories

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ ExclusionDTO, FileDTO, SchedulingDTO, TaskDTO }
import api.services.{ DayType, PeriodType, SchedulingType }
import api.utils.DateUtils._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.mappings.SchedulingMappings._
import database.repositories.exclusion.ExclusionRepository
import database.repositories.scheduling.SchedulingRepository
import database.repositories.task.TaskRepository
import org.scalatest._
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class SchedulingRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
  private implicit val taskRepo: TaskRepository = injector.instanceOf[TaskRepository]
  private implicit val schedulingRepo: SchedulingRepository = injector.instanceOf[SchedulingRepository]
  private val dtbase: Database = injector.instanceOf[Database]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val fileUUID1: String = UUID.randomUUID().toString
  private val fileUUID2: String = UUID.randomUUID().toString

  private val taskUUID1: String = UUID.randomUUID().toString
  private val taskUUID2: String = UUID.randomUUID().toString
  private val taskUUID3: String = UUID.randomUUID().toString

  private val schedulingUUID1: String = UUID.randomUUID().toString
  private val schedulingUUID2: String = UUID.randomUUID().toString
  private val schedulingUUID3: String = UUID.randomUUID().toString
  private val schedulingUUID4: String = UUID.randomUUID().toString

  override def beforeAll: Unit = {
    val result = for {
      _ <- dtbase.run(createFilesTableAction)
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp))
      _ <- dtbase.run(createTasksTableAction)
      _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss"))))
      _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2050 12:00:00", "dd-MM-yyyy HH:mm:ss"))))
      _ <- taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5)))
      res <- dtbase.run(createSchedulingsTableAction)
    } yield res
    Await.result(result, Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  override def afterEach: Unit = {
    Await.result(schedulingRepo.deleteAllSchedulings, Duration.Inf)
  }

  "DBSchedulingsTable#drop/createSchedulingsTable" should {
    "create and then drop the Schedulings table on the database." in {
      val result = for {
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("schedulings") && item.tail.tail.head.name.name.equals("tasks")))
        _ <- dtbase.run(dropSchedulingsTableAction)
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("tasks")))
        _ <- dtbase.run(createSchedulingsTableAction)
        elem <- dtbase.run(MTable.getTables)
      } yield elem
      result.map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("schedulings") && item.tail.tail.head.name.name.equals("tasks")))
    }
  }

  "DBSchedulingsTable#insertInSchedulingsTable,selectAllSchedulings" should {
    "insert rows into the Scheduling table on the database and select all rows" in {
      val result = for {
        _ <- schedulingRepo.selectAllSchedulings.map(seq => assert(seq.isEmpty))
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        resultSeq <- schedulingRepo.selectAllSchedulings
      } yield resultSeq
      result.map(seq => assert(seq.size == 2))
    }
  }

  "DBSchedulingsTable#selectSchedulingsBySchedulingId" should {
    "insert several rows and select a specific scheduling by giving its schedulingId" in {
      val result = for {
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- schedulingRepo.selectSchedulingById(schedulingUUID2).map(dto => assert(dto.get.day.contains(10)))
        task <- schedulingRepo.selectSchedulingById(schedulingUUID1)
      } yield task
      result.map(dto => assert(dto.get.taskId == taskUUID3))
    }
  }

  "DBSchedulingsTable#deleteAllSchedulings" should {
    "insert several rows and then delete them all from the Schedulings table on the database." in {
      val result = for {
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- schedulingRepo.selectAllSchedulings.map(seq => assert(seq.size == 2))
        _ <- schedulingRepo.deleteAllSchedulings
        resultSeq <- schedulingRepo.selectAllSchedulings
      } yield resultSeq
      result.map(seq => assert(seq.isEmpty))
    }
  }

  "DBSchedulingsTable#deleteSchedulingById" should {
    "insert several rows and delete a specific scheduling by giving its schedulingId" in {
      val result = for {
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- schedulingRepo.insertInSchedulingsTable(SchedulingDTO(schedulingUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- schedulingRepo.deleteSchedulingById(schedulingUUID2)
        res <- schedulingRepo.selectAllSchedulings
      } yield res
      result.map(seq => assert(seq.head.schedulingId == schedulingUUID1))
    }
  }

}
