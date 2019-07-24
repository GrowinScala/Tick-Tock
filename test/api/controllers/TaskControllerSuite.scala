package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.TaskDTO
import api.services.{ PeriodType, SchedulingType }
import api.utils.DateUtils._
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import database.repositories.exclusion.ExclusionRepository
import database.repositories.file.FileRepository
import database.repositories.scheduling.SchedulingRepository
import database.repositories.task.TaskRepository
import executionengine.{ ExecutionManager, FakeExecutionManager }
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.{ ExecutionContext, Future }

//TODO implements missing tests
class TaskControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = mock[FileRepository]
  private implicit val taskRepo: TaskRepository = mock[TaskRepository]
  private implicit val exclusionRepo: ExclusionRepository = mock[ExclusionRepository]
  private implicit val schedulingRepo: SchedulingRepository = mock[SchedulingRepository]
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  private val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val LOCALHOST = "localhost:9000"

  private val task1 = TaskDTO("asd1", "test1", SchedulingType.RunOnce, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")))
  private val task2 = TaskDTO("asd2", "test2", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Minutely), Some(2), Some(stringToDateFormat("01-01-2050 12:00:00", "dd-MM-yyyy HH:mm:ss")))
  private val task3 = TaskDTO("asd3", "test3", SchedulingType.Periodic, Some(stringToDateFormat("01-01-2030 12:00:00", "dd-MM-yyyy HH:mm:ss")), Some(PeriodType.Hourly), Some(1), None, Some(5), Some(5))
  private val seqTasks = Seq(task1, task2, task3)

  when(fileRepo.existsCorrespondingFileName("test1")).thenReturn(Future.successful(true))
  when(fileRepo.selectFileIdFromFileName("test1")).thenReturn(Future.successful("asd1"))

  when(exclusionRepo.insertInExclusionsTable(any)).thenReturn(Future.successful(true))

  when(schedulingRepo.insertInSchedulingsTable(any)).thenReturn(Future.successful(true))

  when(taskRepo.insertInTasksTable(any)).thenReturn(Future.successful(true))
  when(taskRepo.selectAllTasks).thenReturn(Future.successful(seqTasks))
  when(taskRepo.selectTask(any)).thenReturn(Future.successful(Some(task1)))
  when(taskRepo.updateTaskById(any, any)).thenReturn(Future.successful(true))
  when(taskRepo.deleteTaskById(any)).thenReturn(Future.successful(1))

  "TaskController#schedule (POST /task)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.schedule.apply(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe "Task received => http://" + LOCALHOST + "/task/asd1"
    }

    "be invalid in" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "unknownKey":"unknownValue"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.schedule.apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"

    }
  }

  "TaskController#getSchedule (GET /task)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(GET, s"/task")
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getSchedule.apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(seqTasks)
    }
  }

  "TaskController#getScheduleById (GET /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(GET, s"/task/asd1" + id)
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getScheduleById(id).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(task1)
    }
  }

  "TaskController#updateTask (PATCH /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, s"/task/$id")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "toDelete": [],
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }

    "be invalid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, s"/task/$id")
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "taskId":"newUUID"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Error replacing scheduled task : \nList((/toDelete,List(JsonValidationError(List(error.path.missing),WrappedArray()))))"
    }
  }

  "TaskController#deleteTask (DELETE /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(DELETE, s"/task/$id")
      val taskController = new TaskController(cc)
      val result = taskController.deleteTask(id).apply(fakeRequest)

      status(result) mustBe NO_CONTENT
    }
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, s"/task/$id")
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "startDateAndTime": "2020-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id).apply(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }

    "be invalid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, s"/task/$id")
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "unknownKey":"unknownValue"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"
    }
  }
}
