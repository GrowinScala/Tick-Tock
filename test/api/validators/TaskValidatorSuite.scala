package api.validators

import java.util.Calendar

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.{ TaskDTO, _ }
import api.services.{ Criteria, DayType, PeriodType, SchedulingType }
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import api.validators.Error._
import com.google.inject.Guice
import database.repositories.file.{ FakeFileRepository, FileRepository }
import database.repositories.task.{ FakeTaskRepository, TaskRepository }
import executionengine.{ ExecutionManager, FakeExecutionManager }
import org.scalatest.{ AsyncWordSpec, MustMatchers, WordSpec }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import api.utils.DateUtils._

import scala.concurrent.{ ExecutionContext, Future }

class TaskValidatorSuite extends AsyncWordSpec with MustMatchers {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)

  private implicit val fileRepo: FileRepository = new FakeFileRepository
  private implicit val taskRepo: TaskRepository = new FakeTaskRepository
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val validator = new TaskValidator
  private val calendar = Calendar.getInstance()

  "TaskValidator#scheduleValidator" should {

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with no startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce)
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce))

    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:00:00"))
      val startDate = stringToDateFormat(dto.startDateAndTime.get, "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(startDate)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate and timezone)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:00:00"), None, None, None, None, Some("PST"))
      val startDate = stringToDateFormat("2030-01-01 20:00:00", "yyyy-MM-dd HH:mm:ss") // 8 hours later due to the PST timezone
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(startDate), None, None, None, None, None, Some("PST")))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task without startDate and with endDate" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, None, Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      val endDate = stringToDateFormat(dto.endDateAndTime.get, "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, None, Some(PeriodType.Minutely), Some(1), Some(endDate)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task with startDate and endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(1), Some(endDate)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Hourly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(2), None, Some(5))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(2), None, Some(5), Some(5)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Daily Periodic task with endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Daily), Some(3), Some(endDate)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Weekly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(4), None, Some(4))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Weekly), Some(4), None, Some(4), Some(4)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Monthly Periodic task with endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(5), Some("2040-01-01 12:00:00"))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val endDate = stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Monthly), Some(5), Some(endDate)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Yearly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Yearly), Some(6), None, Some(3))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Yearly), Some(6), None, Some(3), Some(3)))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with exclusionDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(Some("2030-01-01 12:10:00")))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val exclusionDate = stringToDateFormat("2030-01-01 12:10:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", Some(exclusionDate))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(10)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, Some(10))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, Some(3)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, Some(3))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, Some(DayType.Weekday)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, Some(DayType.Weekday))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, Some(5)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, Some(5))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, None, Some(2031)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, None, Some(2031))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(15), None, None, None, None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, Some(15), None, None, None, None, Some(Criteria.First))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, Some(5), None, None, None, Some(Criteria.Second)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, Some(5), None, None, None, Some(Criteria.Second))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, Some(9), None, Some(Criteria.Fourth)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, Some(9), None, Some(Criteria.Fourth))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, None, Some(2032), Some(Criteria.Last)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")

      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, None, Some(2032), Some(Criteria.Last))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with multiple complex exclusions)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(13), Some(6), None, None, Some(2032)), CreateExclusionDTO(Some("2030-12-25 00:00:00")), CreateExclusionDTO(None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val exclusionDate = stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)), ExclusionDTO("asd1", "asd1", Some(exclusionDate)), ExclusionDTO("asd1", "asd1", None, Some(13), Some(6), None, None, Some(2032))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with schedulingDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(Some("2035-01-01 12:00:00")))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val schedulingDate = stringToDateFormat("2035-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", Some(schedulingDate), None, None, None, None, None, None)))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, Some(15)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, Some(15), None, None, None, None, None)))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, Some(1), None, None, None, None))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, Some(1), None, None, None, None)))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, Some(DayType.Weekday)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, Some(DayType.Weekday))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, Some(10)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, Some(10), None, None)))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2033)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, None, Some(2033), None)))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, Some(31), None, None, None, None, Some(Criteria.First)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, Some(31), None, None, None, None, Some(Criteria.First))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, Some(7), None, None, None, Some(Criteria.Second)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, Some(7), None, None, None, Some(Criteria.Second))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, Some(2), None, Some(Criteria.Fourth)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, Some(2), None, Some(Criteria.Fourth))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2038), Some(Criteria.Last)))))
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, None, Some(2038), Some(Criteria.Last))))))
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with exclusions and timezone)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(1), None, Some(24), Some("PST"), Some(List(CreateExclusionDTO(Some("2035-01-01 00:00:00")))),
        Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2038), Some(Criteria.Last)))))
      val startDatePST = stringToDateFormat("2030-01-01 20:00:00", "yyyy-MM-dd HH:mm:ss")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDatePST), Some(PeriodType.Monthly), Some(1), None, Some(24), Some(24), Some("PST"), Some(List(ExclusionDTO("asd1", "asd1", Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, None, Some(2038), Some(Criteria.Last))))))
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without any other fields)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic)
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period type or period)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period type)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), None, Some(3), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), None, Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without endDate or occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(2))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat))
    }

    "receive an invalid CreateTaskDTO with invalid startDate format." in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030:01:01 12-00-00"))
      val dto2 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-14-01 12:00:00"))
      val dto3 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-32 12:00:00"))
      val dto4 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 25:00:00"))
      val dto5 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:61:00"))

      for {
        validation1 <- validator.scheduleValidator(dto1)
        validation2 <- validator.scheduleValidator(dto2)
        validation3 <- validator.scheduleValidator(dto3)
        validation4 <- validator.scheduleValidator(dto4)
        validation5 <- validator.scheduleValidator(dto5)
      } yield {
        validation1 mustBe Left(List(invalidStartDateFormat))
        validation2 mustBe Left(List(invalidStartDateFormat))
        validation3 mustBe Left(List(invalidStartDateFormat))
        validation4 mustBe Left(List(invalidStartDateFormat))
        validation5 mustBe Left(List(invalidStartDateFormat))
      }
    }

    "receive an invalid CreateTaskDTO with invalid startDate values." in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2019-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidStartDateValue))
    }

    "receive an invalid CreateTaskDTO with invalid file name." in {
      val dto = CreateTaskDTO("test5", SchedulingType.RunOnce)
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidFileName))
    }

    "receive an invalid CreateTaskDTO with invalid task type." in {
      val dto = CreateTaskDTO("test1", "Unknown")
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidCreateTaskFormat, invalidTaskType))
    }

    "receive an invalid CreateTaskDTO with invalid period type." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some("Unknown"), Some(3), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidPeriodType))
    }

    "receive an invalid CreateTaskDTO with invalid period." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(-1), Some("2040-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidPeriod))
    }

    "receive an invalid CreateTaskDTO with invalid endDate format." in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(1), Some("2040|01|01 12/00/00"))
      val dto2 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-14-01 12:00:00"))
      val dto3 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-32 12:00:00"))
      val dto4 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 25:00:00"))
      val dto5 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:61:00"))

      for {
        validation1 <- validator.scheduleValidator(dto1)
        validation2 <- validator.scheduleValidator(dto2)
        validation3 <- validator.scheduleValidator(dto3)
        validation4 <- validator.scheduleValidator(dto4)
        validation5 <- validator.scheduleValidator(dto5)
      } yield {
        validation1 mustBe Left(List(invalidEndDateFormat))
        validation2 mustBe Left(List(invalidEndDateFormat))
        validation3 mustBe Left(List(invalidEndDateFormat))
        validation4 mustBe Left(List(invalidEndDateFormat))
        validation5 mustBe Left(List(invalidEndDateFormat))
      }
    }

    "receive an invalid CreateTaskDTO with invalid endDate values." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2025-01-01 12:00:00"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidEndDateValue))
    }

    "receive an invalid CreateTaskDTO with invalid occurrences." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), None, Some(-1))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidOccurrences))
    }

    "receive an invalid CreateTaskDTO with invalid timezone." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, Some("BDT"))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidTimezone))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => no parameters)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO())))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => exclusionDate + another parameter)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 00:00:00"), Some(15)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => only criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, None, None, Some(Criteria.Fourth)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionFormat))
    }

    //TODO this
    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusionDate format)" in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035|01|01 00/00/00")))))
      val dto2 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-14-01 00:00:00")))))
      val dto3 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-32 00:00:00")))))
      val dto4 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 25:00:00")))))
      val dto5 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 00:61:00")))))

      for {
        validation1 <- validator.scheduleValidator(dto1)
        validation2 <- validator.scheduleValidator(dto2)
        validation3 <- validator.scheduleValidator(dto3)
        validation4 <- validator.scheduleValidator(dto4)
        validation5 <- validator.scheduleValidator(dto5)
      } yield {
        validation1 mustBe Left(List(invalidExclusionDateFormat))
        validation2 mustBe Left(List(invalidExclusionDateFormat))
        validation3 mustBe Left(List(invalidExclusionDateFormat))
        validation4 mustBe Left(List(invalidExclusionDateFormat))
        validation5 mustBe Left(List(invalidExclusionDateFormat))
      }
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusionDate values)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2045-01-01 00:00:00")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionDateValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, Some(32)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionDayValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, Some(8)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionDayOfWeekValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, Some("Holiday")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionDayTypeValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, Some(13)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionMonthValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, None, Some(1995)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionYearValue))
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, Some(20), None, None, None, None, Some("Fifth")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidExclusionCriteriaValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => no parameters)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO())))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => schedulingDate + another parameter)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2035-01-01 00:00:00"), None, None, None, Some(10)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => only criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, None, None, Some(Criteria.Third)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingFormat))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid schedulingDate format)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2035:01:01 00:00:00")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingDateFormat))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid schedulingDate values)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2045-01-01 00:00:00")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingDateValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, Some(0)))))

      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingDayValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, Some(8)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingDayOfWeekValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, Some("Christmas")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingDayTypeValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, Some(13)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingMonthValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2006)))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingYearValue))
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, Some(5), None, None, None, None, Some("Sixth")))))
      for {
        validation <- validator.scheduleValidator(dto)
      } yield validation mustBe Left(List(invalidSchedulingCriteriaValue))
    }

  }

}
