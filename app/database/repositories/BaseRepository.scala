package database.repositories

import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent._
import scala.concurrent.duration._

trait BaseRepository {

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): Future[T] = db.run(action)
}
