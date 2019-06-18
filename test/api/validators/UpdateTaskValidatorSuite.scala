package api.validators

import java.util.{ Calendar, UUID }

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ TaskDTO, _ }
import api.services.{ Criteria, DayType, PeriodType, SchedulingType }
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import api.validators.Error._
import com.google.inject.Guice
import database.repositories.file.{ FakeFileRepository, FileRepository, FileRepositoryImpl }
import database.repositories.task.{ FakeTaskRepository, TaskRepository, TaskRepositoryImpl }
import executionengine.{ ExecutionManager, FakeExecutionManager }
import org.scalatest._
import play.api.inject.guice.GuiceApplicationBuilder
import api.utils.DateUtils._
import database.mappings.ExclusionMappings._
import database.mappings.FileMappings._
import database.mappings.SchedulingMappings._
import database.mappings.TaskMappings._
import database.repositories.exclusion.{ ExclusionRepository, ExclusionRepositoryImpl, FakeExclusionRepository }
import database.repositories.scheduling.{ FakeSchedulingRepository, SchedulingRepository, SchedulingRepositoryImpl }
import database.utils.DatabaseUtils.TEST_DB
import slick.jdbc.H2Profile.api._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.Duration

class UpdateTaskValidatorSuite extends AsyncWordSpec with MustMatchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private val dtbase: Database = appBuilder.injector.instanceOf[Database]
  private implicit val fileRepo: FileRepository = new FileRepositoryImpl(TEST_DB)
  private implicit val exclusionRepo: ExclusionRepository = new ExclusionRepositoryImpl(TEST_DB)
  private implicit val schedulingRepo: SchedulingRepository = new SchedulingRepositoryImpl(TEST_DB)
  private implicit val taskRepo: TaskRepository = new TaskRepositoryImpl(TEST_DB, exclusionRepo, schedulingRepo)
  private implicit val uuidGen: UUIDGenerator = appBuilder.injector.instanceOf[UUIDGenerator]
  private implicit val executionManager: ExecutionManager = appBuilder.injector.instanceOf[ExecutionManager]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val validator = new TaskValidator
  private val calendar = Calendar.getInstance()

  private val fileUUID1: String = UUID.randomUUID().toString
  private val fileUUID2: String = UUID.randomUUID().toString
  private val fileUUID3: String = UUID.randomUUID().toString
  private val fileUUID4: String = UUID.randomUUID().toString

  private val taskUUID1: String = UUID.randomUUID().toString
  private val taskUUID2: String = UUID.randomUUID().toString
  private val taskUUID3: String = UUID.randomUUID().toString
  private val taskUUID4: String = UUID.randomUUID().toString

  private val exclusionUUID1: String = UUID.randomUUID().toString
  private val exclusionUUID2: String = UUID.randomUUID().toString
  private val exclusionUUID3: String = UUID.randomUUID().toString

  private val schedulingUUID1: String = UUID.randomUUID().toString
  private val schedulingUUID2: String = UUID.randomUUID().toString

  override def beforeAll: Unit = {
    Await.result(dtbase.run(createFilesTableAction), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp)), Duration.Inf)
    Await.result(dtbase.run(createTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(createExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(createSchedulingsTableAction), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID1, "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")))), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID2, "test2", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2040 12:00:00", "dd-MM-yyyy HH:mm:ss")), None, None, None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID2, None, Some(15), None, Some(DayType.Weekday)))))), Duration.Inf)
    Await.result(taskRepo.insertInTasksTable(TaskDTO(taskUUID3, "test3", SchedulingType.Personalized, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Weekly), Some(1), None, Some(5), Some(5), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID3, Some(stringToDateFormat("07-01-2030 00:00:00", "dd-MM-yyyy HH:mm:ss"))))))), Duration.Inf)
  }

  override def afterAll: Unit = {
    Await.result(dtbase.run(dropSchedulingsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropExclusionsTableAction), Duration.Inf)
    Await.result(dtbase.run(dropTasksTableAction), Duration.Inf)
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  "TaskValidator#updateValidator" should {

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with no startDate)" in {
      val dto = UpdateTaskDTO(List("startDateAndTime"), Some(taskUUID4), Some("test4"), Some(SchedulingType.RunOnce))
      val result = for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation
      result.foreach(println(_))
      result.map(_ mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.RunOnce)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test4"), Some(SchedulingType.RunOnce), Some("2030-01-01 12:00:00"))
      val startDate = stringToDateFormat(dto.startDateAndTime.get, "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.RunOnce, Some(startDate)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate and timezone)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test4"), Some(SchedulingType.RunOnce), Some("2030-01-01 12:00:00"), None, None, None, None, Some("PST"))
      val startDate = stringToDateFormat("2030-01-01 20:00:00", "yyyy-MM-dd HH:mm:ss") // 8 hours later due to the PST timezone
      for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.RunOnce, Some(startDate), None, None, None, None, None, Some("PST")))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task without startDate and with endDate" in {
      val dto = UpdateTaskDTO(List("startDateAndTime", "exclusions"), Some(taskUUID4), Some("test4"), Some(SchedulingType.Periodic), None, Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      val endDate = stringToDateFormat(dto.endDateAndTime.get, "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.Periodic, None, Some(PeriodType.Minutely), Some(1), Some(endDate)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task with startDate and endDate)" in {
      val dto = UpdateTaskDTO(List("exclusions"), Some(taskUUID4), Some("test4"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(1), Some(endDate)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Hourly Periodic task with occurrences)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime", "exclusions"), Some(taskUUID4), Some("test4"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(2), None, Some(5))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test4", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(2), None, Some(5), Some(5)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Daily Periodic task with endDate)" in {
      val dto = UpdateTaskDTO(List("exclusions"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Daily), Some(3), Some(endDate)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Weekly Periodic task with occurrences)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime", "exclusions"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(4), None, Some(4))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Weekly), Some(4), None, Some(4), Some(4)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Monthly Periodic task with endDate)" in {
      val dto = UpdateTaskDTO(List("exclusions"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(5), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Monthly), Some(5), Some(endDate)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Yearly Periodic task with occurrences)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime", "exclusions"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Yearly), Some(6), None, Some(3))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Yearly), Some(6), None, Some(3), Some(3)))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with exclusionDate)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2030-01-01 12:10:00")))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val exclusionDate = stringToDateFormat("2030-01-01 12:10:00", "yyyy-MM-dd HH:mm:ss")
      println(dto)
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, Some(exclusionDate))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, Some(10)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, Some(10))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, Some(3)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, Some(3))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, Some(DayType.Weekday)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, Some(DayType.Weekday))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, Some(5)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, None, Some(5))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, None, Some(2031)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, None, None, Some(2031))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, Some(15), None, None, None, None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, Some(15), None, None, None, None, Some(Criteria.First))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, Some(5), None, None, None, Some(Criteria.Second)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, Some(5), None, None, None, Some(Criteria.Second))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, Some(9), None, Some(Criteria.Fourth)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, None, Some(9), None, Some(Criteria.Fourth))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, None, Some(2032), Some(Criteria.Last)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")

      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID1, taskUUID4, None, None, None, None, None, Some(2032), Some(Criteria.Last))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with multiple complex exclusions)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, Some(13), Some(6), None, None, Some(2032)), UpdateExclusionDTO(Some(exclusionUUID2), Some(taskUUID4), Some("2030-12-25 00:00:00")), UpdateExclusionDTO(Some(exclusionUUID3), Some(taskUUID4), None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val exclusionDate = stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO(exclusionUUID3, taskUUID4, None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)), ExclusionDTO(exclusionUUID2, taskUUID4, Some(exclusionDate)), ExclusionDTO(exclusionUUID1, taskUUID4, None, Some(13), Some(6), None, None, Some(2032))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with schedulingDate)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), Some("2035-01-01 12:00:00")))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val schedulingDate = stringToDateFormat("2035-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, Some(schedulingDate), None, None, None, None, None, None)))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, Some(15)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, Some(15), None, None, None, None, None)))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, Some(1)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, Some(1), None, None, None, None)))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, Some(DayType.Weekday)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, Some(DayType.Weekday))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, Some(10)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, None, Some(10), None, None)))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, None, Some(2033)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, None, None, Some(2033), None)))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, Some(31), None, None, None, None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, Some(31), None, None, None, None, Some(Criteria.First))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, Some(7), None, None, None, Some(Criteria.Second)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, Some(7), None, None, None, Some(Criteria.Second))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, Some(2), None, Some(Criteria.Fourth)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, None, Some(2), None, Some(Criteria.Fourth))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year and criteria)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, None, Some(2038), Some(Criteria.Last)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, None, None, Some(2038), Some(Criteria.Last))))))
    }

    "receive a valid UpdateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with exclusions and timezone)" in {
      val dto = UpdateTaskDTO(List("endDateAndTime"), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), Some("PST"), Some(List(UpdateExclusionDTO(Some("2030-01-01 00:00:00")))),
        Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, None, Some(2038), Some(Criteria.Last)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Right(TaskDTO(taskUUID4, "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), Some("PST"), None, Some(List(SchedulingDTO(schedulingUUID1, taskUUID4, None, None, None, None, None, Some(2038), Some(Criteria.Last))))))
    }

    "receive an invalid UpdateTaskDTO with missing fields. (Periodic task without any other fields)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid UpdateTaskDTO with missing fields. (Periodic task without period type or period)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid UpdateTaskDTO with missing fields. (Periodic task without period type)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), None, Some(3), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid UpdateTaskDTO with missing fields. (Periodic task without period)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), None, Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid UpdateTaskDTO with missing fields. (Periodic task without endDate or occurrences)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(2))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid startDate format." in {
      val dto1 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.RunOnce), Some("2030:01:01 12-00-00"))
      val dto2 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.RunOnce), Some("2030-14-01 12:00:00"))
      val dto3 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.RunOnce), Some("2030-01-32 12:00:00"))
      val dto4 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.RunOnce), Some("2030-01-01 25:00:00"))
      val dto5 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.RunOnce), Some("2030-01-01 12:61:00"))

      for {
        validation1 <- validator.updateValidator("asd1", dto1)
        validation2 <- validator.updateValidator("asd2", dto2)
        validation3 <- validator.updateValidator("asd3", dto3)
        validation4 <- validator.updateValidator("asd4", dto4)
        validation5 <- validator.updateValidator("asd5", dto5)
      } yield {
        validation1 mustBe Left(List(invalidStartDateFormat))
        validation2 mustBe Left(List(invalidStartDateFormat))
        validation3 mustBe Left(List(invalidStartDateFormat))
        validation4 mustBe Left(List(invalidStartDateFormat))
        validation5 mustBe Left(List(invalidStartDateFormat))
      }
    }

    "receive an invalid UpdateTaskDTO with invalid startDate values." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.RunOnce), Some("2019-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation mustBe Left(List(invalidStartDateValue))
    }

    "receive an invalid UpdateTaskDTO with invalid file name." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test5"), Some(SchedulingType.RunOnce))
      for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation mustBe Left(List(invalidFileName))
    }

    "receive an invalid UpdateTaskDTO with invalid task type." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some("Unknown"))
      for {
        validation <- validator.updateValidator(taskUUID1, dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat, invalidTaskType))
    }

    "receive an invalid UpdateTaskDTO with invalid period type." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some("Unknown"), Some(3), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidPeriodType))
    }

    "receive an invalid UpdateTaskDTO with invalid period." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(-1), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidPeriod))
    }

    "receive an invalid UpdateTaskDTO with invalid endDate format." in {
      val dto1 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(1), Some("2040|01|01 12/00/00"))
      val dto2 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-14-01 12:00:00"))
      val dto3 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-32 12:00:00"))
      val dto4 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 25:00:00"))
      val dto5 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:61:00"))

      for {
        validation1 <- validator.updateValidator("asd1", dto1)
        validation2 <- validator.updateValidator("asd1", dto2)
        validation3 <- validator.updateValidator("asd1", dto3)
        validation4 <- validator.updateValidator("asd1", dto4)
        validation5 <- validator.updateValidator("asd1", dto5)
      } yield {
        validation1 mustBe Left(List(invalidEndDateFormat))
        validation2 mustBe Left(List(invalidEndDateFormat))
        validation3 mustBe Left(List(invalidEndDateFormat))
        validation4 mustBe Left(List(invalidEndDateFormat))
        validation5 mustBe Left(List(invalidEndDateFormat))
      }
    }

    "receive an invalid UpdateTaskDTO with invalid endDate values." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2025-01-01 12:00:00"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidEndDateValue))
    }

    "receive an invalid UpdateTaskDTO with invalid occurrences." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), None, Some(-1))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidOccurrences))
    }

    "receive an invalid UpdateTaskDTO with invalid timezone." in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, Some("BDT"))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidTimezone))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid exclusion format => no parameters)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid exclusion format => exclusionDate + another parameter)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035-01-01 00:00:00"), Some(15)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid exclusion format => only criteria)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, None, None, Some(Criteria.Fourth)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    //TODO this
    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid exclusionDate format)" in {
      val dto1 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035|01|01 00/00/00")))))
      val dto2 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035-14-01 00:00:00")))))
      val dto3 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035-01-32 00:00:00")))))
      val dto4 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035-01-01 25:00:00")))))
      val dto5 = UpdateTaskDTO(List(), Some("asd1"), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2035-01-01 00:61:00")))))

      for {
        validation1 <- validator.updateValidator("asd1", dto1)
        validation2 <- validator.updateValidator("asd1", dto2)
        validation3 <- validator.updateValidator("asd1", dto3)
        validation4 <- validator.updateValidator("asd1", dto4)
        validation5 <- validator.updateValidator("asd1", dto5)
      } yield {
        validation1 mustBe Left(List(invalidExclusionDateFormat))
        validation2 mustBe Left(List(invalidExclusionDateFormat))
        validation3 mustBe Left(List(invalidExclusionDateFormat))
        validation4 mustBe Left(List(invalidExclusionDateFormat))
        validation5 mustBe Left(List(invalidExclusionDateFormat))
      }
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid exclusionDate values)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), Some("2045-01-01 00:00:00")))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionDateValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid day)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, Some(32)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionDayValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid dayOfWeek)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, Some(8)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionDayOfWeekValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid dayType)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, Some("Holiday")))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionDayTypeValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid month)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, Some(13)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionMonthValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid year)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, None, None, None, None, Some(1995)))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionYearValue))
    }

    "receive an invalid UpdateTaskDTO with invalid exclusions. (invalid criteria)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Periodic), Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(UpdateExclusionDTO(Some(exclusionUUID1), Some(taskUUID4), None, Some(20), None, None, None, None, Some("Fifth")))))
      for {
        validation <- validator.updateValidator(taskUUID2, dto)
      } yield validation mustBe Left(List(invalidExclusionCriteriaValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid scheduling format => no parameters)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid scheduling format => schedulingDate + another parameter)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), Some("2035-01-01 00:00:00"), None, None, None, Some(10)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid scheduling format => only criteria)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, None, None, Some(Criteria.Third)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid schedulingDate format)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), Some("2035:01:01 00:00:00")))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingDateFormat))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid schedulingDate values)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), Some("2045-01-01 00:00:00")))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingDateValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid day)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, Some(0)))))

      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingDayValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid dayOfWeek)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, Some(8)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingDayOfWeekValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid dayType)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, Some("Christmas")))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingDayTypeValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid month)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, Some(13)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingMonthValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid year)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, None, None, None, None, Some(2006)))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingYearValue))
    }

    "receive an invalid UpdateTaskDTO with invalid schedulings. (invalid criteria)" in {
      val dto = UpdateTaskDTO(List(), Some(taskUUID4), Some("test1"), Some(SchedulingType.Personalized), Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(UpdateSchedulingDTO(Some(schedulingUUID1), Some(taskUUID4), None, Some(5), None, None, None, None, Some("Sixth")))))
      for {
        validation <- validator.updateValidator(taskUUID3, dto)
      } yield validation mustBe Left(List(invalidSchedulingCriteriaValue))
    }
  }
}
