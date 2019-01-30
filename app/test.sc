import java.sql.Timestamp
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

println(new Timestamp(stringToDateFormat("01-01-2019 12:00:00", "dd-MM-yyyy HH:mm:ss").getTime).getTime)
println(new Timestamp(stringToDateFormat("01-02-2019 12:00:00", "dd-MM-yyyy HH:mm:ss").getTime).getTime)
println(new Timestamp(stringToDateFormat("01-03-2019 12:00:00", "dd-MM-yyyy HH:mm:ss").getTime).getTime)
println(new Timestamp(stringToDateFormat("01-04-2019 12:00:00", "dd-MM-yyyy HH:mm:ss").getTime).getTime)
