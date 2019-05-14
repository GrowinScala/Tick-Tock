import api.utils.DateUtils.stringToDateFormat

/*
import akka.actor.Cancellable
import api.services.SchedulingType
import executionengine.ExecutionJob
import api.utils.DateUtils._
import database.repositories.{FileRepository, TaskRepository}
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
lazy val injector: Injector = appBuilder.injector()
implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
implicit val fileRepo: FileRepository = injector.instanceOf[FileRepository]
implicit val taskRepo: TaskRepository = injector.instanceOf[TaskRepository]

var cancellableMap: scala.collection.mutable.Map[String, Cancellable] = scala.collection.mutable.Map[String, Cancellable]()
cancellableMap += ("1" -> new ExecutionJob("1", "fileId", SchedulingType.RunOnce, Some(stringToDateFormat("2030-01-01 12:00:00" , "yyyy-MM-dd HH:mm:ss"))).start)
cancellableMap.head */

import api.utils.DateUtils._
val futureDate = getDateWithAddedSeconds(3)

println(futureDate)