package database.repositories

import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent._
import scala.concurrent.duration._

/**
  * Trait that defines a repository. Contains methods/values that are in common for all repositories.
  */
trait BaseRepository {

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): Future[T] = db.run(action)
}
