import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class TaskControllerSuite extends PlaySpec with GuiceOneAppPerSuite {

  /**
    * new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    * new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    * new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    * new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    */

  "TaskController#index" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "TaskController#schedule" should {
    "receive a POST request with a JSON body with the correct data. (yyyy-MM-dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data. (dd-MM-yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01-07-2019 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data. (yyyy/MM/dd HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "2019/07/01 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data. (dd/MM/yyyy HH:mm:ss date format)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/07/2019 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a POST request with a JSON body with the correct data. (max delay exceeded)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/01/2030 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a POST request with a JSON body with incorrect data. (wrong file name)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/01/2030 00:00:00",
            "fileName": "Unknown"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date format)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01:01:2030 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong date values)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/14/2030 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (wrong time values)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/01/2030 25:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }

    "receive a POST request with a JSON body with incorrect data. (given date already happened)" in {
      val fakeRequest = FakeRequest(POST, s"/task")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse("""
          {
            "startDateAndTime": "01/01/2015 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "TaskController#GETtask" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/task")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

}
