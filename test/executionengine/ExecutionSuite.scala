package executionengine

import java.io.ByteArrayOutputStream
import java.util.{ Calendar, Date }

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import api.dtos.{ ExclusionDTO, SchedulingDTO, TaskDTO }
import api.services.{ PeriodType, SchedulingType, TaskService }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }
import api.utils.DateUtils._
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import database.repositories.file.{ FakeFileRepository, FileRepository }
import database.repositories.task.{ FakeTaskRepository, TaskRepository }
import executionengine.ExecutionJob._
import java.time.Duration

import slick.jdbc.MySQLProfile.api._

import scala.collection.mutable

class ExecutionSuite extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll with Matchers {

  private implicit val fileRepo: FileRepository = new FakeFileRepository
  private implicit val taskRepo: TaskRepository = new FakeTaskRepository
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  "ExecutionActor#Start" should {
    "start a task with date that needs to be delayed." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30000000)
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.Delaying)
      system.stop(actorRef)
    }

    "start a runOnce task and receive the corresponding message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.RunOnceWaiting)
      system.stop(actorRef)
    }

    "start a periodic task and receive the corresponding message. (with endDate)" in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicWaiting)
      system.stop(actorRef)
    }

    "start a periodic task and receive the corresponding message. (with occurrences)" in {
      val fileId = "test3"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd3", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, task.timezone, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicWaiting)
      system.stop(actorRef)
    }

    "start a periodic task and receive the corresponding message. (with exclusions)" in {
      val fileId = "test1"
      val currentDate = new Date()
      val startDate = getDateWithAddedSeconds(currentDate, 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Minutely), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, None, Some(List(ExclusionDTO("dsa1", "asd1", Some(getDateWithAddedSeconds(currentDate, 60))))))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, task.timezone, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicWaiting)
      system.stop(actorRef)
    }

    "start a personalized task and receive the corresponding message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Personalized, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None) // needed schedulings also but not needed for the test (we can give the date queue directly to the actor)
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, List(getDateWithAddedSeconds(new Date(), 60)), Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PersonalizedWaiting)
      system.stop(actorRef)
    }
  }

  "ExecutionActor#Execute" should {
    "execute a RunOnce task and receive the expected message." in {
      val fileId = "test1"
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! ExecuteRunOnce
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.RunOnceRunning)
      system.stop(actorRef)
    }

    "execute a Periodic task and receive the expected message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 500000)
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(getDateWithAddedSeconds(new Date(), 1000000)))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! ExecutePeriodic
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicRunning)
      system.stop(actorRef)
    }

    "execute a Personalized task and receive the expected message." in {
      val fileId = "test1"
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val task = TaskDTO("asd1", fileId, SchedulingType.Personalized, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None) // needed schedulings also but not needed for the test (we can give the date queue directly to the actor)
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, List(getDateWithAddedSeconds(new Date(), 60)), Nil, fileRepo, taskRepo, executionManager))
      actorRef ! ExecutePersonalized
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PersonalizedRunning)
      system.stop(actorRef)
    }
  }

  "ExecutionActor#Delay" should {
    "delay a task and receive the expected message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30000000)
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.Delaying)
      system.stop(actorRef)
    }
  }

  "ExecutionActor#Cancel" should {
    "cancel an ongoing task and receive the expected message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Nil, Nil, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! Cancel
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.Canceled)
      system.stop(actorRef)
    }
  }
}
