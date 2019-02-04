package executionengine

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import api.services.SchedulingType
import com.google.inject.Guice
import database.repositories.{FakeFileRepository, FakeTaskRepository, FileRepository, TaskRepository}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.inject.guice.GuiceApplicationBuilder
import api.utils.DateUtils._

import scala.concurrent.ExecutionContext

class ExecutionSuite extends TestKit(ActorSystem("SchedulerSystem")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach{

  /*implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  implicit val fileRepo: FileRepository = new FakeFileRepository
  implicit val taskRepo: TaskRepository = new FakeTaskRepository
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo Actor" should {
    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "Hello World!"
      expectMsg("Hello World!")
    }
  }

  "ExecutionJob#start" should {
    "receive a simple RunOnce task" in {
      val fileId = "123"
      new ExecutionJob("test", fileId, SchedulingType.RunOnce)(fileRepo, taskRepo).start
      expectMsg("Ran file " + fileId + " scheduled to run immediately.")
    }
  }*/
}