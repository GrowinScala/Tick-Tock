import api.utils.{ FakeUUIDGenerator, UUIDGenerator }
import com.google.inject.AbstractModule
import database.repositories.{ FileRepository, FileRepositoryImpl, TaskRepository, TaskRepositoryImpl }
import database.utils.DatabaseUtils._
import slick.jdbc.MySQLProfile.api._

class Module extends AbstractModule {

  def configure(): Unit = {
    bind(classOf[Database]).toInstance(TEST_DB)
    bind(classOf[FileRepository]).toInstance(new FileRepositoryImpl(TEST_DB))
    bind(classOf[TaskRepository]).toInstance(new TaskRepositoryImpl(TEST_DB))
    bind(classOf[UUIDGenerator]).toInstance(new FakeUUIDGenerator)
  }
}