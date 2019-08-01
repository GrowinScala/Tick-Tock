package api.controllers

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import api.dtos.FileDTO
import api.utils.DateUtils.stringToDateFormat
import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import database.repositories.file.FileRepository
import executionengine.{ ExecutionManager, FakeExecutionManager }
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, MustMatchers }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{ ControllerComponents, Results }
import play.api.test.Helpers._
import play.api.test._
import generators.Generator

import scala.concurrent.{ ExecutionContext, Future }

class FileControllerSuite extends PlaySpec with Results with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with MustMatchers with MockitoSugar {

  private lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  private lazy val injector: Injector = appBuilder.injector()
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private implicit val fileRepo: FileRepository = mock[FileRepository]
  private implicit val UUIDGen: UUIDGenerator = new FakeUUIDGenerator
  private implicit val executionManager: ExecutionManager = new FakeExecutionManager
  private val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private implicit val actorSystem: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = ActorMaterializer()

  private val gen: Generator = new Generator

  private val id1: String = gen.id
  private val id2: String = gen.id
  private val id3: String = gen.id

  private val fileName1: String = gen.fileName
  private val fileName2: String = gen.fileName
  private val fileName3: String = gen.fileName

  private val file1: FileDTO = FileDTO(id1, fileName1, stringToDateFormat("2018-02-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))
  private val file2: FileDTO = FileDTO(id2, fileName2, stringToDateFormat("2018-03-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))
  private val file3: FileDTO = FileDTO(id3, fileName3, stringToDateFormat("2018-02-01 12:00:00", "yyyy-MM-dd HH:mm:ss"))

  private val seqFiles: Seq[FileDTO] = Seq(file1, file2, file3)

  when(fileRepo.selectAllFiles(any, any)).thenReturn(Future.successful(seqFiles))

  "FileController#getAllFiles" should {
    "receive a GET request with several files" in {
      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getAllFiles(None, None).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(seqFiles)
    }

    "receive a GET request with no files" in {
      val emptySeq: Seq[FileDTO] = Seq()
      when(fileRepo.selectAllFiles(any, any)).thenReturn(Future.successful(emptySeq))

      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getAllFiles(None, None).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(emptySeq)
    }
  }

  when(fileRepo.selectFileById(id1)).thenReturn(Future.successful(Some(file1)))

  "FileController#getFileById" should {
    "receive a valid GET request" in {
      val fakeRequest = FakeRequest(GET, s"/file/$id1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getFileById(id1).apply(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(file1)
    }

    "receive an invalid GET request" in {
      when(fileRepo.selectFileById(id1)).thenReturn(Future.successful(None))

      val fakeRequest = FakeRequest(GET, s"/file/$id1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.getFileById(id1).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
    }
  }

  when(fileRepo.deleteFileById(id1)).thenReturn(Future.successful(1))

  "FileController#deleteFile" should {
    "receive a valid DELETE request" in {
      when(fileRepo.selectFileById(id1)).thenReturn(Future.successful(Some(file1)))
      val fakeRequest = FakeRequest(DELETE, s"/file/$id1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.deleteFile(id1).apply(fakeRequest)

      status(result) mustBe NO_CONTENT
    }

    "receive an invalid DELETE request" in {
      when(fileRepo.selectFileById(id1)).thenReturn(Future.successful(None))

      val fakeRequest = FakeRequest(DELETE, s"/file/$id1")
        .withHeaders(HOST -> "localhost:9000")
      val fileController = new FileController(cc)
      val result = fileController.deleteFile(id1).apply(fakeRequest)

      status(result) mustBe BAD_REQUEST
    }
  }

}
