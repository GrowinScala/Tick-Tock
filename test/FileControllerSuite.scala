import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{BadPart, FilePart}
import play.api.test._
import play.api.test.Helpers._

class FileControllerSuite extends PlaySpec with GuiceOneAppPerSuite {

  "FileController#file" should {

    "receive a POST request with a .jar file and fileName correctly." in {
      /*val files = Seq[FilePart[TemporaryFile]](FilePart("file", "UploadServiceSpec.scala", None, TemporaryFile("file", "spec")))
      val multipartBody = MultipartFormData(Map[String, Seq[String]](), files, Seq[BadPart](), Seq[MissingFilePart]())
      val fakeRequest = FakeRequest(POST, s"/file")
        .withHeaders(HOST -> "localhost:9000")
        .withJsonBody(Json.parse(
          """
          {
            "startDateAndTime": "2019-07-01 00:00:00",
            "fileName": "test"
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK*/
    }

    "receive a POST request with a .jar file only. (missing fileName)" in {

    }

    "receive a POST request with a file that isn't a .jar and a fileName. (wrong file extension)" in {

    }

    "receive a POST request with a .jar file and a fileName that already exists. (given existing fileName)" in {

    }
  }
}
