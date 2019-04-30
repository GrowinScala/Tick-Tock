package executionengine

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ExecutionSuite extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll with Matchers{

}
