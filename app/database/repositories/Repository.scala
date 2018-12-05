package database.repositories

import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent._
import scala.concurrent.duration._

trait Repository {

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)
}
