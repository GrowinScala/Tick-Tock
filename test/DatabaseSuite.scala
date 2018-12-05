import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import slick.jdbc.H2Profile.api._
import database.repositories.FileRepository._
import database.repositories.TaskRepository._
import slick.dbio.DBIO

import scala.concurrent._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class DatabaseSuite extends FunSuite {

  val db = Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1",
    driver="org.h2.Driver")

  def testExec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  test("Database - FilesTable: create and drop table."){
    createFilesTable
    assert()
  }

  test("Database - FilesTable: insert and delete all from table."){

  }

  test("Database - FilesTable: insert and select by id from table."){

  }

  test("Database - FilesTable: insert and select by name from table."){

  }

  test("Database - FilesTable: insert and select by path from table."){

  }

  test("Database - FilesTable: insert and update by id."){

  }

  test("Database - FilesTable: insert and update by name."){

  }

  test("Database - FilesTable: insert and update by path."){

  }

  test("Database - FilesTable: insert and delete by id."){

  }

  test("Database - FilesTable: insert and delete by name."){

  }

  test("Database - FilesTable: insert and delete by path."){

  }

  test("Database - FilesTable: existsCorrespondingFileId."){

  }

  test("Database - FilesTable: existsCorrespondingFileName."){

  }

  test("Database - FilesTable: selectFileIdFromName."){

  }

  test("Database - FilesTable: selectNameFromFileId."){

  }

  test("Database: insert a file and then a task."){

  }

  test("Database: insert a task and then a file."){

  }

  test("Database: insert a file and task " +
    "and find if the foreign key fileId exists."){


  }

  test("Database - TasksTable: create and drop table."){

  }

  test("Database - TasksTable: insert and delete all from table."){

  }

  test("Database - TasksTable: insert and select by taskId from table."){

  }

  test("Database - TasksTable: insert and select by fileId from table."){

  }

  test("Database - TasksTable: insert and select by datetime from table."){

  }

  test("Database - TasksTable: insert and update by taskId."){

  }

  test("Database - TasksTable: insert and update by fileId."){

  }

  test("Database - TasksTable: insert and update by datetime."){

  }

  test("Database - TasksTable: insert and delete by taskId."){

  }

  test("Database - TasksTable: insert and delete by fileId."){

  }

  test("Database - TasksTable: insert and delete by datetime."){

  }


}
