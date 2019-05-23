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
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.Delaying)
      system.stop(actorRef)
    }

    "start a runOnce task and receive the corresponding message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(startDate))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.RunOnceWaiting)
      system.stop(actorRef)
    }

    "start a periodic task and receive the corresponding message. (with startDate)" in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicWaiting)
      system.stop(actorRef)
    }

    "start a periodic task and receive the corresponding message. (with occurrences)" in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), None, Some(3), Some(3))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, fileRepo, taskRepo, executionManager))
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
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, task.totalOccurrences, task.currentOccurrences, task.timezone, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicWaiting)
      system.stop(actorRef)
    }

    "start a personalized task and receive the corresponding message." in {
      val fileId = "test1"
      val startDate = getDateWithAddedSeconds(new Date(), 30)
      val task = TaskDTO("asd1", fileId, SchedulingType.Personalized, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None) // needed schedulings also but not needed for the test (we can give the date queue directly to the actor)
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Some(mutable.Queue(getDateWithAddedSeconds(new Date(), 60))), None, fileRepo, taskRepo, executionManager))
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
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! ExecuteRunOnce
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.RunOnceRunning)
      system.stop(actorRef)
    }

    "execute a Periodic task and receive the expected message." in {
      val fileId = "test1"
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")))
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, Some(Duration.ofHours(1)), task.endDateAndTime, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! ExecutePeriodic
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.PeriodicRunning)
      system.stop(actorRef)
    }

    "execute a Personalized task and receive the expected message." in {
      val fileId = "test1"
      val startDate = stringToDateFormat("2030-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")
      val task = TaskDTO("asd1", fileId, SchedulingType.Personalized, Some(startDate), Some(PeriodType.Hourly), Some(1), Some(stringToDateFormat("2040-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss")), None) // needed schedulings also but not needed for the test (we can give the date queue directly to the actor)
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, Some(mutable.Queue(getDateWithAddedSeconds(new Date(), 60))), None, fileRepo, taskRepo, executionManager))
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
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, None, None, fileRepo, taskRepo, executionManager))
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
      val actorRef = system.actorOf(Props(classOf[ExecutionJob], task.taskId, task.fileName, task.taskType, task.startDateAndTime, None, None, None, None, None, fileRepo, taskRepo, executionManager))
      actorRef ! Start
      actorRef ! Cancel
      actorRef ! GetStatus
      expectMsg(ExecutionStatus.Canceled)
      system.stop(actorRef)
    }
  }

  /*
  "ExecutionJob" should {
    "run single run task immediately." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce)
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(1000)
      }
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(1) + "] Ran file " + fileId + " scheduled to run immediately."))
    }

    "run single run task at the correct startDate." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(getDateWithAddedSeconds(10)))
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(10000)
      }
      assert(stream.toString.contains("[" + getCurrentDate + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getCurrentDate, "yyyy-MM-dd HH:mm:ss").toString + ".\n"))
    }

    "run single run task at the correct startDate with timezone." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.RunOnce, Some(getDateWithSubtractedSeconds((3600 * 7))), None, None, None, None, None, Some("PST"))
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(10000)
      }
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(10) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getCurrentDate, "yyyy-MM-dd HH:mm:ss").toString + ".\n"))
    }

    "run periodic task at the correct dates." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(getDateWithAddedSeconds(10)), Some(PeriodType.Minutely), Some(1), Some(getDateWithAddedSeconds(10 + (60 * 2))))
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(10000 + (60000 * 2))
      }
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(130) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(130), "yyyy-MM-dd HH:mm:ss") + "."))
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(70) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(70), "yyyy-MM-dd HH:mm:ss") + "."))
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(10) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(10), "yyyy-MM-dd HH:mm:ss") + "."))
    }

    "run periodic task at the correct dates with a date exception." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.Periodic, Some(getDateWithAddedSeconds(10)), Some(PeriodType.Minutely), Some(1), Some(getDateWithAddedSeconds(10 + (60 * 2))), None, None, None, Some(List(ExclusionDTO("dsa1", "asd1", Some(getDateWithAddedSeconds(70))))))
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(10000 + (60000 * 2))
      }
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(130) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(130), "yyyy-MM-dd HH:mm:ss") + "."))
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(10) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(10), "yyyy-MM-dd HH:mm:ss") + "."))
    }

    "run personalized task at the correct dates." in {
      val fileId = "asd1"
      val stream = new ByteArrayOutputStream
      Console.withOut(stream) {
        val task = TaskDTO("asd1", fileId, SchedulingType.Personalized, Some(getDateWithAddedSeconds(10)), Some(PeriodType.Minutely), Some(1), None, Some(5), Some(5), None, None, Some(List(SchedulingDTO("dsa1", "asd1", Some(getDateWithAddedSeconds(70))))))
        val service = new TaskService
        service.scheduleTask(task)
        Thread.sleep(10000 + 60000)
      }
      assert(stream.toString.contains("[" + getDateWithSubtractedSeconds(70) + "] Ran file " + fileId + " scheduled to run at " + dateToStringFormat(getDateWithSubtractedSeconds(70), "yyyy-MM-dd HH:mm:ss") + "."))
    }
  }
   */

}
