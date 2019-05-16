package database.repositories

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ ExclusionDTO, FileDTO, TaskDTO }
import api.services.{ DayType, PeriodType, SchedulingType }
import api.utils.DateUtils._
import database.mappings.FileMappings._
import database.mappings.TaskMappings._
import database.mappings.ExclusionMappings._
import database.repositories.exclusion.ExclusionRepository
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import org.scalatest._
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class ExclusionRepositorySuite extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
  private implicit val taskRepo: TaskRepository = injector.instanceOf[TaskRepository]
  private implicit val exclusionRepo: ExclusionRepository = injector.instanceOf[ExclusionRepository]
  private val dtbase: Database = injector.instanceOf[Database]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val fileUUID1: String = UUID.randomUUID().toString
  private val fileUUID2: String = UUID.randomUUID().toString

  private val taskUUID1: String = UUID.randomUUID().toString
  private val taskUUID2: String = UUID.randomUUID().toString
  private val taskUUID3: String = UUID.randomUUID().toString

  private val exclusionUUID1: String = UUID.randomUUID().toString
  private val exclusionUUID2: String = UUID.randomUUID().toString
  private val exclusionUUID3: String = UUID.randomUUID().toString
  private val exclusionUUID4: String = UUID.randomUUID().toString

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(dtbase.run(createTasksTableAction), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")))), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2050 12:00:00", "dd-MM-yyyy HH:mm:ss")))), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))), Duration.Inf)
    Await.result(dtbase.run(createExclusionsTableAction), Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  override def afterEach: Unit = {
    Await.result(exclusionRepo.deleteAllExclusions, Duration.Inf)
  }

  "DBExclusionsTable#drop/createExclusionsTable" should {
    "create and then drop the Exclusions table on the database." in {
      for {
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("exclusions") && item.tail.head.name.name.equals("files") && item.tail.tail.head.name.name.equals("tasks")))
        _ <- dtbase.run(dropExclusionsTableAction)
        _ <- dtbase.run(MTable.getTables).map(item => assert(item.head.name.name.equals("files") && item.tail.head.name.name.equals("tasks")))
        _ <- dtbase.run(createExclusionsTableAction)
        result <- dtbase.run(MTable.getTables)
      } yield {
        result.head.name.name mustBe "exclusions"
        result.tail.head.name.name mustBe "files"
        result.tail.tail.head.name.name mustBe "tasks"
      }

    }
  }

  "DBExclusionsTable#insertInExclusionsTable,selectAllExclusions" should {
    "insert rows into the Exclusions table on the database and select all rows" in {
      for {
        _ <- exclusionRepo.selectAllExclusions.map(seq => assert(seq.isEmpty))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        resultSeq <- exclusionRepo.selectAllExclusions
      } yield resultSeq.size mustBe 2
    }
  }

  "DBExclusionsTable#selectExclusion" should {
    "insert several rows and select a specific exclusion by giving its exclusionId" in {
      for {
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- exclusionRepo.selectExclusion(exclusionUUID2).map(dto => assert(dto.get.day.contains(10)))
        exclusion <- exclusionRepo.selectExclusion(exclusionUUID1)
      } yield exclusion.get.taskId mustBe taskUUID3
    }
  }

  "DBExclusionsTable#selectExclusionByTaskId" should {
    "insert several rows and select a specific exclusion by giving its taskId" in {
      for {
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- exclusionRepo.selectExclusionsByTaskId(taskUUID3).map(elem => assert(elem.get.size == 1 && elem.get.head.exclusionId == exclusionUUID1))
        _ <- exclusionRepo.selectExclusionsByTaskId(taskUUID2).map(elem => assert(elem.get.isEmpty))
        exclusionList <- exclusionRepo.selectExclusionsByTaskId(taskUUID1)
      } yield exclusionList.get.head.dayType mustBe DayType.Weekday
    }
  }

  "DBExclusionsTable#deleteAllExclusions" should {
    "insert several rows and then delete them all from the Exclusions table on the database." in {
      for {
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- exclusionRepo.selectAllExclusions.map(seq => assert(seq.size == 2))
        _ <- exclusionRepo.deleteAllExclusions
        resultSeq <- exclusionRepo.selectAllExclusions
      } yield resultSeq.isEmpty mustBe true
    }
  }

  "DBExclusionsTable#deleteExclusionById" should {
    "insert several rows and delete a specific exclusion by giving its exclusionId" in {
      for {
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID1, taskUUID3, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
        _ <- exclusionRepo.insertInExclusionsTable(ExclusionDTO(exclusionUUID2, taskUUID1, None, Some(10), None, Some(DayType.Weekday), None, Some(2030)))
        _ <- exclusionRepo.deleteExclusionById(exclusionUUID2)
        resultSeq <- exclusionRepo.selectAllExclusions
      } yield resultSeq.head.exclusionId mustBe exclusionUUID1
    }
  }

}
