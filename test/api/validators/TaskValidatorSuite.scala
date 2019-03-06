package api.validators

import java.util.{Calendar, UUID}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.{CreateTaskDTO, TaskDTO}
import api.services.{PeriodType, SchedulingType}
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.validators.Error._
import com.google.inject.Guice
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

class TaskValidatorSuite extends PlaySpec{

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  implicit val fileRepo: FileRepository = new FakeFileRepository
  implicit val taskRepo: TaskRepository = new FakeTaskRepository
  implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val validator = new TaskValidator
  val calendar = Calendar.getInstance()

  "TaskValidator#scheduleValidator" should {
    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with no startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce)
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1" , "test1", SchedulingType.RunOnce)).toString
    }

    "receive a valid CreateTaskDTO, succeed in the validation and convert it to a TaskDTO. (RunOnce task with a startDate)" in {
      val dto = CreateTaskDTO("test1", SchedulingType.RunOnce, Some("2030-01-01 12:00:00"))
      calendar.set(2030, 1 - 1, 1, 12, 0, 0)
      val startDate = calendar.getTime
      validator.scheduleValidator(dto).toString mustBe Right(TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(startDate))).toString
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

  }


}