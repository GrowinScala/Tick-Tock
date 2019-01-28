package api.controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.{FileDTO, TaskDTO}
import api.services.SchedulingType
import database.repositories.{FileRepository, FileRepositoryImpl, TaskRepository, TaskRepositoryImpl}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Mode}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import api.utils.DateUtils._
import play.api.test.FakeRequest
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import com.google.inject.{AbstractModule, Guice}
import database.utils.DatabaseUtils.TEST_DB
import play.mvc.Result
import play.api.test.Helpers.{route, _}
import play.api.test._
import play.mvc.Http._
import slick.jdbc.meta.MTable


class TaskFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach{

  /**
    * new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    * new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    * new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    * new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    */

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  implicit val fileRepo: FileRepository = appBuilder.injector.instanceOf[FileRepository]
  implicit val taskRepo: TaskRepository = appBuilder.injector.instanceOf[TaskRepository]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val LOCALHOST = "localhost:9000"

  val taskUUID1: String = UUID.randomUUID().toString
  val taskUUID2: String = UUID.randomUUID().toString
  val taskUUID3: String = UUID.randomUUID().toString
  val taskUUID4: String = UUID.randomUUID().toString

  val fileUUID1: String = UUID.randomUUID().toString
  val fileUUID2: String = UUID.randomUUID().toString
  val fileUUID3: String = UUID.randomUUID().toString
  val fileUUID4: String = UUID.randomUUID().toString

  override def beforeAll = {
    val result = for {
      _ <- fileRepo.createFilesTable
      a <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", getCurrentDateTimestamp))
      b <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", getCurrentDateTimestamp))
      c <- fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", getCurrentDateTimestamp))
      res <- fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", getCurrentDateTimestamp))
    } yield (a,b,c,res)
    result.foreach(println(_))
    Await.result(result, Duration.Inf)
    Await.result(taskRepo.createTasksTable, Duration.Inf)
    println(Await.result(TEST_DB.run(MTable.getTables), Duration.Inf))
    println(Await.result(fileRepo.selectAllFiles, Duration.Inf))
  }

  override def beforeEach = {

  }

  override def afterAll = {
    Await.result(taskRepo.dropTasksTable, Duration.Inf)
    Await.result(fileRepo.dropFilesTable, Duration.Inf)
  }

  override def afterEach = {
    Await.result(taskRepo.deleteAllTasks, Duration.Inf)
  }

  "TaskController#index" should {
    "receive a GET request"  in {
      val fakeRequest = FakeRequest(GET, "/")
        .withHeaders(HOST -> LOCALHOST)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "POST /task" should {
    "receive a POST request with a JSON body with the correct data and insert it into the database. (yyyy-MM-dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (dd-MM-yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01-07-2019 00:00:00",
            "fileName": "test2",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (yyyy/MM/dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019/07/01 00:00:00",
            "fileName": "test3",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (dd/MM/yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/07/2019 00:00:00",
            "fileName": "test4",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data and insert it into the database. (max delay exceeded)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2030-01-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with incorrect data. (wrong file name)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "Unknown",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01:07:2019 00:00:00",
            "fileName": "test2",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date values)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-14-01 00:00:00",
            "fileName": "test3",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong time values)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 25:00:00",
            "fileName": "test4",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (given date already happened)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2015-01-01 00:00:00",
            "fileName": "test1",
            "taskType": "RunOnce"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (with endDateAndTime) (Minutely) (yyyy-MM-dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test2",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020-01-01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (with occurrences) (Minutely)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test3",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Hourly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test4",
            "taskType": "Periodic",
            "periodType": "Hourly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Daily)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "Periodic",
            "periodType": "Daily",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Weekly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test2",
            "taskType": "Periodic",
            "periodType": "Weekly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Monthly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test3",
            "taskType": "Periodic",
            "periodType": "Monthly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (Yearly)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test4",
            "taskType": "Periodic",
            "periodType": "Yearly",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (dd-MM-yyyy HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "01-01-2020 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (yyyy/MM/dd HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test2",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020/01/01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with correct periodic task data and insert it into the database. (dd/MM/yyyy HH:mm:ss endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test3",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "01/01/2020 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 1)
      status(routeOption.get) mustBe OK
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing Periodic fields)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test4",
            "taskType": "Periodic"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (negative period)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": -1,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (wrong endDate format)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test2",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2020:01:01 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (given endDate already happened)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-01-01 00:00:00",
            "fileName": "test3",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2019-01-15 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (given endDate happens before startDate)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test4",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "endDateAndTime": "2019-06-15 00:00:00"
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (negative occurrences)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test1",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2,
            "occurrences": -1
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing periodType field)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test2",
            "taskType": "Periodic",
            "period": 2,
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing period field)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test3",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "occurrences": 5
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect periodic task data. (missing endDate/occurrences)" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test4",
            "taskType": "Periodic",
            "periodType": "Minutely",
            "period": 2
          }
        """))
      val routeOption = route(app, fakeRequest)
      val result = for{
        routeResult <- routeOption.get
        selectResult <- taskRepo.selectAllTasks
      } yield (routeResult, selectResult)
      result.map(tuple => tuple._2.size mustBe 0)
      status(routeOption.get) mustBe BAD_REQUEST
    }
  }

  /*"TaskController#GETtask" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(POST, "/task")
        .withHeaders(HOST -> LOCALHOST)
      val result = route(app, fakeRequest)
      val bodyText: String = contentAsString(result.get)
      println(bodyText)
      status(result.get) mustBe OK
    }
  }*/

}
