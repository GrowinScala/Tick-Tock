package api.services

import api.dtos.{SchedulingDTO, TaskDTO}
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import api.utils.DateUtils._
import executionengine.ExecutionJob

import scala.concurrent.ExecutionContext

class TaskServiceSuite extends PlaySpec{

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fileRepo: FileRepository = new FakeFileRepository
  implicit val taskRepo: TaskRepository = new FakeTaskRepository

  val taskService = new TaskService()

  "TaskService#scheduleTask" should {

    "receive a RunOnce TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.RunOnce, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap + (taskDTO.taskId -> new ExecutionJob(taskDTO.taskId, taskDTO.fileName, taskDTO.taskType, taskDTO.startDateAndTime, None, None, None, None, None).start)
      println(taskService.cancellableMap + "ola")
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Minutely Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Minutely), Some(5), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Hourly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Hourly), Some(4), None, Some(24), Some(24))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Daily Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Daily), Some(3), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Weekly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Weekly), Some(2), None, Some(12), Some(12))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Monthly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Monthly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Yearly Periodic TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Periodic, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), Some(PeriodType.Yearly), Some(1), None, Some(5), Some(5))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

    "receive a Personalized TaskDTO and store a new entry in the cancellableMap." in {
      taskService.cancellableMap.isEmpty mustBe true
      val taskDTO = TaskDTO("asd", "asd", SchedulingType.Personalized, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss")), None, None, Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, None, Some(List(SchedulingDTO("asd1", "asd", Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))))
      taskService.scheduleTask(taskDTO)
      taskService.cancellableMap.size mustBe 1
      taskService.cancellableMap.head._1 mustBe "asd"
      taskService.cancellableMap - "asd"
    }

  }

  /*def scheduleTask(task: TaskDTO): Unit ={
    fileRepo.selectFileIdFromFileName(task.fileName).map{ fileId =>
      task.taskType match{
        case SchedulingType.RunOnce =>
          cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.RunOnce, task.startDateAndTime, None, None, task.timezone, calculateExclusions(task), None).start)
        case SchedulingType.Periodic =>
          task.periodType.get match {
            case PeriodType.Minutely =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofMinutes(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
            case PeriodType.Hourly =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofHours(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
            case PeriodType.Daily =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
            case PeriodType.Weekly =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 7)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
            case PeriodType.Monthly =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 30)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
            case PeriodType.Yearly =>
              cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Periodic, task.startDateAndTime, Some(Duration.ofDays(task.period.get * 365)), task.endDateAndTime, task.timezone, calculateExclusions(task), None).start)
          }
        case SchedulingType.Personalized =>
          cancellableMap += (task.taskId -> new ExecutionJob(task.taskId, fileId, SchedulingType.Personalized, task.startDateAndTime, None, task.endDateAndTime, task.timezone, calculateExclusions(task), calculateSchedulings(task)).start)
      }
    }
  }*/

  "TaskService#replaceTask" should {
   //taskService.cancellableMap += ("asd" -> new ExecutionJob())
  }

  /*def replaceTask(id: String, task: TaskDTO): Unit = {
    if(cancellableMap.contains(id)){
      cancellableMap(id).cancel
      cancellableMap -= id
    }
    scheduleTask(task)
  }*/

  "TaskService#calculateExclusions" should {
    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with exclusionDate)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayOfWeek)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and dayType)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and dayType)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with month, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, month, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a exclusion and return a Queue with the corresponding Date(s) for that exclusion. (with day, dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with several exclusions and return a Queue with the corresponding Dates for those exclusions." in {

    }

  }

  "TaskService#calculateSchedulings" should {
    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with schedulingDate)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayOfWeek)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and dayType)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and dayType)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with month, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, month, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with a scheduling and return a Queue with the corresponding Date(s) for that scheduling. (with day, dayOfWeek, dayType, month, year and criteria)" in {

    }

    "receive a TaskDTO with several schedulings and return a Queue with the corresponding Dates for those schedulings" in {

    }

  }
}
