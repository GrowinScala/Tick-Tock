package api.services

import akka.actor.{ ActorSystem, Props }
import api.dtos.{ ExclusionDTO, SchedulingDTO, TaskDTO }
import api.utils.DateUtils._
import database.repositories.{ FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository }
import executionengine.ExecutionJob
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
      taskService.actorMap += ("asd" -> system.actorOf(Props(classOf[ExecutionJob], "asd", "asd", SchedulingType.RunOnce, Some(stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, None, fileRepo, taskRepo)))
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
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(15)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-15 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(1)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-01-07 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, Some(DayType.Weekend)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-06 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, Some(1)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-01-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2031-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, None, None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-12-30 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayOfWeek)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(5), Some(5)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-05 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-02-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(4), None, Some(DayType.Weekday)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-04 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(10), None, None, Some(6)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2031-06-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and year)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-10-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2032-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, Some(10), None, None, None, Some(2030)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-10-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-11-10 00:00:00", "yyyy-MM-dd HH:mm:ss"), stringToDateFormat("2030-12-10 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and dayType)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(3), Some(DayType.Weekday)))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-03 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and month)" in {
      val dto = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2030-03-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("asd", "asd", None, None, Some(3), None))))
      taskService.calculateExclusions(dto) mustBe Some(scala.collection.mutable.Queue(stringToDateFormat("2030-01-03 00:00:00", "yyyy-MM-dd HH:mm:ss")))
    }

    /*"receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and month)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with an exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with several exclusions and return a Queue with the corresponding Dates for those exclusions." in {

    }

  }

  "TaskService#calculateSchedulings" should {

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with schedulingDate)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayOfWeek)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayType)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and dayType)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a valid TaskDTO with several schedulings and return a Queue with the corresponding Dates for those schedulings" in {

    }*/

  }
}
