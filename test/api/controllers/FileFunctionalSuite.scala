package api.controllers

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.test.Helpers
import play.test.Helpers._

class FileFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite {

  /*"FileController#file" should {

    "receive a POST request with a .jar file and fileName correctly." in {
      val tempFile = java.io.File.createTempFile("testFile", "jar")
      tempFile.deleteOnExit()
      //val files = Seq[FilePart[TemporaryFile]](FilePart("file", "UploadServiceSpec.scala", None, TemporaryFile("file", "spec")))
      //val multipartBody = MultipartFormData(Map[String, Seq[String]](), files, Seq[BadPart](), Seq[MissingFilePart]())
      val fakeRequest = Helpers.fakeRequest(POST, s"/file")
        .withHeaders(HOST -> "localhost:9000")        .withMultipartFormDataBody(files)
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }


    "receive a POST request with a .jar file only. (missing fileName)" in {

    }

    "receive a POST request with a file that isn't a .jar and a fileName. (wrong file extension)" in {

    }

    "receive a POST request with a .jar file and a fileName that already exists. (given existing fileName)" in {

    }

  }*/
}
