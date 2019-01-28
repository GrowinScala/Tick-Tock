package executionengine

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

class ExecutionSuite extends TestKit(ActorSystem("SchedulerActor")) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach{

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "An Echo Actor" must {
    "send back messages unchanged" in {
      val echo = system.actorOf(TestActors.echoActorProps)
      echo ! "Hello World!"
      expectMsg("Hello World!")
    }
  }
}
