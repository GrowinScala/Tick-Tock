package services

import java.io.{FileInputStream, InputStream}

import org.flywaydb.core._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

// import slick.jdbc.JdbcBackend.Database
import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.duration._

object TickTock {

  case class ScheduledTask(
                            idScheduling: Int,
                            idFile: Int,
                            startDate: String,
                            endDate: String,
                            period: String,
                            occurences: Int,
                            exception: String,
                            status: String
                          )

  /*case class Test(
                 field1: String,
                 field2: String
                 )*/

  class ScheduledTaskTable(tag: Tag) extends Table[ScheduledTask](tag, "scheduledtask"){
    def idScheduling = column[Int]("idScheduling")
    def idFile = column[Int]("idFile")
    def startDate = column[String]("startDate")
    def endDate = column[String]("endDate")
    def period = column[String]("period")
    def occurences = column[Int]("occurences")
    def exception = column[String]("exception")
    def status = column[String]("status")

    def * = (idScheduling, idFile, startDate, endDate, period, occurences, exception, status) <> (ScheduledTask.tupled, ScheduledTask.unapply)
  }

  /*class TestTable(tag: Tag) extends Table[Test](tag, "test"){
    def field1 = column[String]("field1")
    def field2 = column[String]("field2")

    def * = (field1, field2) <> (Test.tupled, Test.unapply)
  }*/

  lazy val scheduledTaskTable = TableQuery[ScheduledTaskTable]
  /*lazy val testTable = TableQuery[TestTable]*/

  val createTableAction = scheduledTaskTable.schema.create
  /*val createTestAction = testTable.schema.create*/

  val selectScheduledTasksAction = scheduledTaskTable.result

  val db = Database.forConfig("dbinfo")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def main(args: Array[String]): Unit = {

    val flyway: Flyway = Flyway.configure().dataSource(
      "jdbc:mysql://localhost:3306/ticktock?serverTimezone=Portugal",
      "root",
      "growin"
    ).load()

    flyway.baseline()
    flyway.migrate()

    //val yaml = new Yaml()
    //val inputStream: InputStream = new FileInputStream("dbinfo.yml")
    //val e: DBInfo = yaml.load(inputStream).asInstanceOf[DBInfo]
    //println(e.url)



    //val yaml: Yaml = new Yaml()
    //val inputStream: InputStream = this.getClass().getClassLoader().getResourceAsStream("dbinfo.yml")
    //val obj: DBInfo = yaml.load(inputStream).asInstanceOf[DBInfo]


    /*exec(createTestAction)*/
    println(exec(selectScheduledTasksAction))

  }




  //val db = DatabaseConfig.forConfig("../../dbinfo.yml")

}
