package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import database.repositories.exclusion.{ExclusionRepository, FakeExclusionRepository}
import database.repositories.file.FileRepository
import database.repositories.scheduling.{FakeSchedulingRepository, SchedulingRepository}
import database.repositories.task.{FakeTaskRepository, TaskRepository}
import executionengine.{ExecutionManager, FakeExecutionManager}
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

import scala.concurrent.{ExecutionContext, Future}

//TODO implements missing tests
class TaskControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val fileRepo: FileRepository = mock[FileRepository]
  private implicit val taskRepo: TaskRepository = new FakeTaskRepository
  private implicit val exclusionRepo: ExclusionRepository = new FakeExclusionRepository
  private implicit val schedulingRepo: SchedulingRepository = new FakeSchedulingRepository
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  private val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val LOCALHOST = "localhost:9000"

  when(fileRepo.existsCorrespondingFileName("test1")).thenReturn(Future.successful(true))
  when(fileRepo.selectFileIdFromFileName("test1")).thenReturn(Future.successful("asd1"))

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
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/asd1"
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
      val bodyText = contentAsString(result)
      bodyText mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"

    }
  }

  "TaskController#getSchedule (GET /task)" should {
    "be valid in" in {
      val fakeRequest = FakeRequest(GET, s"/task")
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getSchedule.apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "[{\"taskId\":\"asd1\",\"fileName\":\"test1\",\"taskType\":\"RunOnce\",\"startDateAndTime\":\"Tue Jan 01 12:00:00 GMT 2030\"}," +
        "{\"taskId\":\"asd2\",\"fileName\":\"test2\",\"taskType\":\"Periodic\",\"startDateAndTime\":\"Tue Jan 01 12:00:00 GMT 2030\",\"periodType\":\"Minutely\",\"period\":2,\"endDateAndTime\":\"Sat Jan 01 12:00:00 GMT 2050\"}," +
        "{\"taskId\":\"asd3\",\"fileName\":\"test3\",\"taskType\":\"Periodic\",\"startDateAndTime\":\"Tue Jan 01 12:00:00 GMT 2030\",\"periodType\":\"Hourly\",\"period\":1,\"totalOccurrences\":5,\"currentOccurrences\":5}]"
    }
  }

  "TaskController#getScheduleById (GET /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(GET, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
      val taskController = new TaskController(cc)
      val result = taskController.getScheduleById(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "{\"taskId\":\"asd1\",\"fileName\":\"test1\",\"taskType\":\"RunOnce\",\"startDateAndTime\":\"Tue Jan 01 12:00:00 GMT 2030\"}"
    }
  }

  "TaskController#updateTask (PATCH /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PATCH, s"/task/" + id)
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
      val fakeRequest = FakeRequest(PATCH, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST, CONTENT_TYPE -> "application/json")
        .withBody(Json.parse("""
          {
            "taskId":"newUUID"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.updateTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "Error replacing scheduled task : \nList((/toDelete,List(JsonValidationError(List(error.path.missing),WrappedArray()))))"
    }
  }

  "TaskController#deleteTask (DELETE /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(DELETE, s"/task/" + id)
      val taskController = new TaskController(cc)
      val result = taskController.deleteTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe ""
    }
  }

  "TaskController#replaceTask (PUT /task/:id)" should {
    "be valid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, s"/task/" + id)
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
      val bodyText = contentAsString(result)
      bodyText mustBe "Task received => http://" + LOCALHOST + "/task/" + id
    }

    "be invalid in" in {
      val id = "asd1"
      val fakeRequest = FakeRequest(PUT, s"/task/" + id)
        .withHeaders(HOST -> LOCALHOST)
        .withBody(Json.parse("""
          {
            "unknownKey":"unknownValue"
          }
        """))
      val taskController = new TaskController(cc)
      val result = taskController.replaceTask(id).apply(fakeRequest)
      val bodyText = contentAsString(result)
      bodyText mustBe "{\"status\":\"Error:\",\"message\":{\"obj.taskType\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}],\"obj.fileName\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}}"
    }
  }
}
