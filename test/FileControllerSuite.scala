import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class FileControllerSuite extends PlaySpec with GuiceOneAppPerSuite {

  "FileController#GETfile" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "FileController#GETfileWithId" should {
    "receive a GET request with a valid id" in {
      val fakeRequest = FakeRequest(GET, s"/file/1")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a GET request with an invalid id" in {
      val fakeRequest = FakeRequest(GET, s"/file/asd")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "FileController#DELETEfileWithId" should {
    "receive a DELETE request with a valid id" in {
      val fakeRequest = FakeRequest(DELETE, s"/file/1")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }

    "receive a DELETE request with an invalid id" in {
      val fakeRequest = FakeRequest(DELETE, s"/file/asd")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }
}
