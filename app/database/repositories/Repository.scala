package database.repositories

import database.mappings.FileMappings.FileRow
import slick.dbio.DBIO
import slick.driver.MySQLDriver.api._

import scala.concurrent._
import scala.concurrent.duration._

trait Repository {

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)
}
