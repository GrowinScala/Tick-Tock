package api.validators

import java.util.Calendar

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos._
import api.services.{Criteria, DayType, PeriodType, SchedulingType}
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.validators.Error._
import com.google.inject.Guice
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

class TaskValidatorSuite extends PlaySpec {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)

  private implicit val fileRepo: FileRepository = new FakeFileRepository
  private implicit val taskRepo: TaskRepository = new FakeTaskRepository
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val validator = new TaskValidator
  private val calendar = Calendar.getInstance()

  "TaskValidator#scheduleValidator" should {
    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with no startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce)
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce)).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:00:00"))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(startDate))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate and timezone)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:00:00"), None, None, None, None, Some("PST"))
      calendar.set(2030, 1 - 1, 1, 20, 0, 0) // 8 hours later due to the PST timezone
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(startDate), None, None, None, None, None, Some("PST"))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task without startDate and with endDate" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, None, Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      calendar.set(2040, 1 - 1, 1, 12, 0, 0)
      val endDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, None, Some(PeriodType.Minutely), Some(1), Some(endDate))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Minutely Periodic task with startDate and endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(1), Some("2040-01-01 12:00:00"))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2040, 1 - 1, 1, 12, 0, 0)
      val endDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(1), Some(endDate))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Hourly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(2), None, Some(5))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(2), None, Some(5), Some(5))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Daily Periodic task with endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:00:00"))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2040, 1 - 1, 1, 12, 0, 0)
      val endDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Daily), Some(3), Some(endDate))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Weekly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(4), None, Some(4))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Weekly), Some(4), None, Some(4), Some(4))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Monthly Periodic task with endDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(5), Some("2040-01-01 12:00:00"))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2040, 1 - 1, 1, 12, 0, 0)
      val endDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Monthly), Some(5), Some(endDate))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Yearly Periodic task with occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Yearly), Some(6), None, Some(3))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Yearly), Some(6), None, Some(3), Some(3))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with exclusionDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(Some("2030-01-01 12:10:00")))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2030, 1 - 1, 1, 12, 10, 0)
      val exclusionDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", Some(exclusionDate)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(10)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, Some(10)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, Some(3)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, Some(3)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, Some(3)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, Some(3)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, Some(5)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, Some(5)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, None, Some(2031)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, None, Some(2031)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with day and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(15), None, None, None, None, Some(Criteria.First)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, Some(15), None, None, None, None, Some(Criteria.First)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayOfWeek and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, Some(5), None, None, None, Some(Criteria.Second)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, Some(5), None, None, None, Some(Criteria.Second)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with dayType and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with month and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, Some(9), None, Some(Criteria.Fourth)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, Some(9), None, Some(Criteria.Fourth)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with exclusions => with year and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, None, None, None, None, Some(2032), Some(Criteria.Last)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, None, None, Some(2032), Some(Criteria.Last)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (with multiple complex exclusions)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(5), None, Some(24), None,
        Some(List(CreateExclusionDTO(None, Some(13), Some(6), None, None, Some(2032)), CreateExclusionDTO(Some("2030-12-25 00:00:00")), CreateExclusionDTO(None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2030, 12 - 1, 25, 0, 0, 0)
      val exclusionDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(5), None, Some(24), Some(24), None, Some(List(ExclusionDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)), ExclusionDTO("asd1", "asd1", Some(exclusionDate)), ExclusionDTO("asd1", "asd1", None, Some(13), Some(6), None, None, Some(2032)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with schedulingDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(Some("2035-01-01 12:00:00")))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2035, 1 - 1, 1, 12, 0, 0)
      val schedulingDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", Some(schedulingDate), None, None, None, None, None, None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, Some(15)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, Some(15), None, None, None, None, None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, Some(1), None, None, None, None))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, Some(1), None, None, None, None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, Some(DayType.Weekday)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, Some(DayType.Weekday), None, None, None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, Some(10)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, Some(10), None, None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2033)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, None, Some(2033), None))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with day and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, Some(31), None, None, None, None, Some(Criteria.First)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, Some(31), None, None, None, None, Some(Criteria.First)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayOfWeek and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, Some(7), None, None, None, Some(Criteria.Second)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, Some(7), None, None, None, Some(Criteria.Second)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with dayType and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with month and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, Some(2), None, Some(Criteria.Fourth)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, Some(2), None, Some(Criteria.Fourth)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with year and criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2038), Some(Criteria.Last)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, None, None, Some(2038), Some(Criteria.Last)))))).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (Personalized task with multiple complex schedulings)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, None, Some(24), None, None,
        Some(List(CreateSchedulingDTO(None, Some(13), Some(6), None, None, Some(2032)), CreateSchedulingDTO(Some("2030-12-25 00:00:00")), CreateSchedulingDTO(None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)))))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      calendar.set(2030, 12 - 1, 25, 0, 0, 0)
      val schedulingDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe
        Right(TaskDTO("asd1", "test1", SchedulingType.Personalized, Some(startDate), None, None, None, Some(24), Some(24), None, None, Some(List(SchedulingDTO("asd1", "asd1", None, None, None, Some(DayType.Weekend), Some(8), None, Some(Criteria.First)), SchedulingDTO("asd1", "asd1", Some(schedulingDate)), SchedulingDTO("asd1", "asd1", None, Some(13), Some(6), None, None, Some(2032)))))).toString
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without any other fields)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic)
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat)).toString
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period type or period)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat)).toString
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period type)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), None, Some(3), Some("2040-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat)).toString
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without period)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), None, Some("2040-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat)).toString
    }

    "receive an invalid CreateTaskDTO with missing fields. (Periodic task without endDate or occurrences)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Monthly), Some(2))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid startDate format." in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030:01:01 12-00-00"))
      val dto2 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-14-01 12:00:00"))
      val dto3 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-32 12:00:00"))
      val dto4 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 25:00:00"))
      val dto5 = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:61:00"))
      validator.scheduleValidator(dto1).toString mustBe Left(List(invalidStartDateFormat)).toString
      validator.scheduleValidator(dto2).toString mustBe Left(List(invalidStartDateFormat)).toString
      validator.scheduleValidator(dto3).toString mustBe Left(List(invalidStartDateFormat)).toString
      validator.scheduleValidator(dto4).toString mustBe Left(List(invalidStartDateFormat)).toString
      validator.scheduleValidator(dto5).toString mustBe Left(List(invalidStartDateFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid startDate values." in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2019-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidStartDateValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid file name." in {
      val dto = CreateTaskDTO("test5", SchedulingType.RunOnce)
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidFileName)).toString
    }

    "receive an invalid CreateTaskDTO with invalid task type." in {
      val dto = CreateTaskDTO("test1", "Unknown")
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidCreateTaskFormat, invalidTaskType)).toString
    }

    "receive an invalid CreateTaskDTO with invalid period type." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some("Unknown"), Some(3), Some("2040-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidPeriodType)).toString
    }

    "receive an invalid CreateTaskDTO with invalid period." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Weekly), Some(-1), Some("2040-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidPeriod)).toString
    }

    "receive an invalid CreateTaskDTO with invalid endDate format." in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(1), Some("2040|01|01 12/00/00"))
      val dto2 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-14-01 12:00:00"))
      val dto3 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-32 12:00:00"))
      val dto4 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 25:00:00"))
      val dto5 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Daily), Some(3), Some("2040-01-01 12:61:00"))
      validator.scheduleValidator(dto1).toString mustBe Left(List(invalidEndDateFormat)).toString
      validator.scheduleValidator(dto2).toString mustBe Left(List(invalidEndDateFormat)).toString
      validator.scheduleValidator(dto3).toString mustBe Left(List(invalidEndDateFormat)).toString
      validator.scheduleValidator(dto4).toString mustBe Left(List(invalidEndDateFormat)).toString
      validator.scheduleValidator(dto5).toString mustBe Left(List(invalidEndDateFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid endDate values." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Hourly), Some(1), Some("2025-01-01 12:00:00"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidEndDateValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid occurrences." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), None, Some(-1))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidOccurrences)).toString
    }

    "receive an invalid CreateTaskDTO with invalid timezone." in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, Some("BDT"))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidTimezone)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => no parameters)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO())))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => exclusionDate + another parameter)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 00:00:00"), Some(15)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusion format => only criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, None, None, Some(Criteria.Fourth)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusionDate format)" in {
      val dto1 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035|01|01 00/00/00")))))
      val dto2 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-14-01 00:00:00")))))
      val dto3 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-32 00:00:00")))))
      val dto4 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 25:00:00")))))
      val dto5 = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2035-01-01 00:61:00")))))
      validator.scheduleValidator(dto1).toString mustBe Left(List(invalidExclusionDateFormat)).toString
      validator.scheduleValidator(dto2).toString mustBe Left(List(invalidExclusionDateFormat)).toString
      validator.scheduleValidator(dto3).toString mustBe Left(List(invalidExclusionDateFormat)).toString
      validator.scheduleValidator(dto4).toString mustBe Left(List(invalidExclusionDateFormat)).toString
      validator.scheduleValidator(dto5).toString mustBe Left(List(invalidExclusionDateFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid exclusionDate values)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(Some("2045-01-01 00:00:00")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionDateValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, Some(32)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionDayValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, Some(8)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionDayOfWeekValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, Some("Holiday")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionDayTypeValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, Some(13)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionMonthValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, None, None, None, None, Some(1995)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionYearValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid exclusions. (invalid criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Periodic, Some("2030-01-01 12:00:00"), Some(PeriodType.Minutely), Some(10), Some("2040-01-01 00:00:00"), None, None, Some(List(CreateExclusionDTO(None, Some(20), None, None, None, None, Some("Fifth")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidExclusionCriteriaValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => no parameters)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO())))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => schedulingDate + another parameter)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2035-01-01 00:00:00"), None, None, None, Some(10)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid scheduling format => only criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, None, None, Some(Criteria.Third)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid schedulingDate format)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2035:01:01 00:00:00")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingDateFormat)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid schedulingDate values)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(Some("2045-01-01 00:00:00")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingDateValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid day)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, Some(0)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingDayValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid dayOfWeek)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, Some(8)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingDayOfWeekValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid dayType)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, Some("Christmas")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingDayTypeValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid month)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, Some(13)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingMonthValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid year)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, None, None, None, None, Some(2006)))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingYearValue)).toString
    }

    "receive an invalid CreateTaskDTO with invalid schedulings. (invalid criteria)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.Personalized, Some("2030-01-01 12:00:00"), None, None, Some("2040-01-01 00:00:00"), None, None, None, Some(List(CreateSchedulingDTO(None, Some(5), None, None, None, None, Some("Sixth")))))
      validator.scheduleValidator(dto).toString mustBe Left(List(invalidSchedulingCriteriaValue)).toString
    }

  }

}
