package api.services

import akka.actor.{ ActorSystem, Props }
import api.dtos.{ ExclusionDTO, SchedulingDTO, TaskDTO }
import api.utils.DateUtils._
import database.repositories.FileRepository
import database.repositories.file.FakeFileRepository
import database.repositories.task.{ FakeTaskRepository, TaskRepository }
import executionengine.{ ExecutionJob, ExecutionManager, FakeExecutionManager }
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

class TaskServiceSuite extends PlaySpec {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = new FakeFileRepository
  private implicit val taskRepo: TaskRepository = new FakeTaskRepository
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager

  private val taskService = new TaskService()
  private val system = ActorSystem("ExecutionSystem")

  "TaskService#scheduleTask" should {

    "receive a RunOnce TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.RunOnce, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Minutely Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Hourly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Hourly), Some(4), None, Some(24), Some(24))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Daily Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(3), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Weekly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Weekly), Some(2), None, Some(12), Some(12))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Monthly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Yearly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Yearly), Some(1), None, Some(5), Some(5))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

    "receive a Personalized TaskDTO and store a new entry in the cancellableMap." in {
      taskService.actorMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd1", "asd", Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))))
      taskService.scheduleTask(taskDTO)
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "asd"
      taskService.actorMap -= "asd"
    }

  }

  "TaskService#replaceTask" should {
    "replace an existing task in the cancellableMap with a new one." in {
      taskService.actorMap.isEmpty mustBe true
      taskService.actorMap += ("asd" -> system.actorOf(Props(classOf[ExecutionJob], "asd", "asd", SchedulingType.RunOnce, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, fileRepo, taskRepo, executionManager)))
      taskService.actorMap.size mustBe 1
      taskService.replaceTask("asd", TaskDTO("dsa", "dsa", SchedulingType.RunOnce, Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))))
      taskService.actorMap.size mustBe 1
      taskService.actorMap.head._1 mustBe "dsa"
      taskService.actorMap -= "dsa"
    }
  }

  "TaskService#calculateExclusions" should {
    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with exclusionDate)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(15)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-15 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(4)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-06 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, Some(0)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(5), Some(7)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(4), None, Some(DayType.Weekday)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(10), None, None, Some(5)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2031-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-10-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(10), None, None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(3), Some(DayType.Weekday)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-08 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-15 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-22 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-29 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(6), None, Some(0)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-11 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-25 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(2), None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-04-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(1)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-02-02 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-03 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-09 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-17 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-24 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend), None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-22 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-29 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, Some(11), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-26 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-27 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-29 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(20), Some(2), Some(DayType.Weekday)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-20 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(19), Some(6), None, Some(6)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-07-19 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(16), Some(4), None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(1), Some(DayType.Weekend), Some(7)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-08-04 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-11 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-25 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(7), Some(DayType.Weekend), None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-07 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-14 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-11-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(10), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-17 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-24 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(10), Some(3), Some(DayType.Weekday), Some(8)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(5), Some(6), Some(DayType.Weekday), None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-05 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-07-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(1), Some(4), None, Some(4), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(30), None, Some(DayType.Weekend), Some(10), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(2), Some(DayType.Weekday), Some(9), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-07 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-14 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(1), Some(6), Some(DayType.Weekday), Some(2), Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(12), None, None, None, None, Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-02-12 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(4), None, None, None, Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekday), None, None, Some(Criteria.Fourth)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, Some(5), None, Some(Criteria.Last)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-06-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2033-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, None, Some(2030), Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(13), Some(4), None, None, None, Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-13 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(7), None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(28), None, None, Some(7), None, Some(Criteria.Fourth)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2033-08-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(23), None, None, None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-23 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(2), Some(DayType.Weekday), None, None, Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(7), None, Some(8), None, Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-14 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(2), None, None, Some(2030), Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-21 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(3), None, Some(Criteria.Fourth)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-14 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, Some(4), Some(2030), Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(17), Some(1), Some(DayType.Weekend), None, None, Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-17 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2060-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(16), Some(6), None, Some(7), None, Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2047-08-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(12), Some(5), None, None, Some(2030), Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-12 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(7), Some(DayType.Weekend), Some(9), None, Some(Criteria.Last)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-26 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(4), Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(3), Some(2030), Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(26), Some(3), Some(DayType.Weekday), Some(10), None, Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-26 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(20), Some(1), Some(DayType.Weekend), None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-20 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(11), Some(2), None, Some(2), Some(2030), Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(9), None, Some(DayType.Weekday), Some(8), Some(2030), Some(Criteria.Fourth)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(1), Some(DayType.Weekend), Some(0), Some(2030), Some(Criteria.First)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-06 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(13), Some(2), Some(DayType.Weekday), Some(4), Some(2030), Some(Criteria.Second)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with several exclusions and return a Queue with the corresponding Dates for those exclusions." in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd1", "asd", None, Some(30), None, Some(DayType.Weekday), None, Some(2030)), ExclusionDTO("asd2", "asd", Some(stringToDateFormat("2030-05-02 00:00:00", "yyyy-MM-dd HH:mm:ss"))), ExclusionDTO("asd3", "asd", None, None, Some(6), None, None, Some(2030), Some(Criteria.Third)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-04-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-05-02 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-05-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-07-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-09-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

  }

  "TaskService#calculateSchedulings" should {

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with schedulingDate)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(15)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-15 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(4)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-06 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, Some(0)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(5), Some(7)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(4), None, Some(DayType.Weekday)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(10), None, None, Some(5)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2031-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-10-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(10), None, None, None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(3), Some(DayType.Weekday)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-08 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-15 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-22 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-29 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(6), None, Some(0)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-11 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-25 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(2), None, None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2030-04-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(1)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-02-02 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-03 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-09 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-17 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-02-24 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend), None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-15 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-22 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-29 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, Some(11), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-25 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-26 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-27 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-29 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(20), Some(2), Some(DayType.Weekday)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-20 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(19), Some(6), None, Some(6)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-07-19 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(16), Some(4), None, None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(1), Some(DayType.Weekend), Some(7)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-08-04 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-11 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-25 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-12-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(7), Some(DayType.Weekend), None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-07 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-14 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-11-15 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(10), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-16 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-17 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-23 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-24 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(10), Some(3), Some(DayType.Weekday), Some(8)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(5), Some(6), Some(DayType.Weekday), None, Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-05 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-07-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(1), Some(4), None, Some(4), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(30), None, Some(DayType.Weekend), Some(10), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(2), Some(DayType.Weekday), Some(9), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-07 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-14 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-21 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(1), Some(6), Some(DayType.Weekday), Some(2), Some(2030)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(12), None, None, None, None, Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-02-12 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(4), None, None, None, Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekday), None, None, Some(Criteria.Fourth)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, Some(5), None, Some(Criteria.Last)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-06-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2033-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, None, Some(2030), Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(13), Some(4), None, None, None, Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-13 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(7), None, Some(DayType.Weekend), None, None, Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(28), None, None, Some(7), None, Some(Criteria.Fourth)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2033-08-28 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(23), None, None, None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-23 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(2), Some(DayType.Weekday), None, None, Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(7), None, Some(8), None, Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-09-14 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(2), None, None, Some(2030), Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-21 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(3), None, Some(Criteria.Fourth)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-14 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekday), None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, None, Some(4), Some(2030), Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-05-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(17), Some(1), Some(DayType.Weekend), None, None, Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-03-17 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2060-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(16), Some(6), None, Some(7), None, Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2047-08-16 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(12), Some(5), None, None, Some(2030), Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-12 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(7), Some(DayType.Weekend), Some(9), None, Some(Criteria.Last)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-26 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(4), Some(DayType.Weekday), None, Some(2030), Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-02 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, None, Some(DayType.Weekend), Some(3), Some(2030), Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-04-07 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(26), Some(3), Some(DayType.Weekday), Some(10), None, Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-11-26 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(20), Some(1), Some(DayType.Weekend), None, Some(2030), Some(Criteria.Last)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-20 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(11), Some(2), None, Some(2), Some(2030), Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(9), None, Some(DayType.Weekday), Some(8), Some(2030), Some(Criteria.Fourth)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, None, Some(1), Some(DayType.Weekend), Some(0), Some(2030), Some(Criteria.First)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-06 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year and criteria)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2031-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd", "asd", None, Some(13), Some(2), Some(DayType.Weekday), Some(4), Some(2030), Some(Criteria.Second)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue())
    }

    "receive a valid TaskDTO with several schedulings and return a Queue with the corresponding Dates for those schedulings." in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd1", "asd", None, Some(30), None, Some(DayType.Weekday), None, Some(2030)), SchedulingDTO("asd2", "asd", Some(stringToDateFormat("2030-05-02 00:00:00", "yyyy-MM-dd HH:mm:ss"))), SchedulingDTO("asd3", "asd", None, None, Some(6), None, None, Some(2030), Some(Criteria.Third)))))
      taskService.calculateSchedulings(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-18 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-04-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-05-02 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-05-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-07-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-08-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-09-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-10-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

  }
}
