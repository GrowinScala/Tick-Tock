import java.util.Date

import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._
import database.repositories.{FileRepositoryImpl, TaskRepositoryImpl}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

implicit val ec = ExecutionContext.Implicits.global
val db = Database.forConfig("dbinfo")
val fileRepo = new FileRepositoryImpl(db)
val taskRepo = new TaskRepositoryImpl(db)

Await.result(fileRepo.existsCorrespondingFileName("test"), Duration.Inf)
Await.result(fileRepo.existsCorrespondingFileName("test2"), Duration.Inf)
