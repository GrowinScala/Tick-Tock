import java.util.Date
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import slick.jdbc.MySQLProfile.api._
import api.utils.DateUtils._

/*import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

implicit val ec = ExecutionContext.Implicits.global
val db = Database.forConfig("dbinfo")
val fileRepo = new FileRepositoryImpl(db)
val taskRepo = new TaskRepositoryImpl(db)*/

//val date = stringToDateFormat("2019-01-04 10:18:00", "yyyy-MM-dd HH:mm:ss")
//isValidDateValue(date)

getCurrentDate