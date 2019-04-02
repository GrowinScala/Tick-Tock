import akka.actor.ActorSystem
import api.utils.{ DefaultUUIDGenerator, UUIDGenerator }
import com.google.inject.AbstractModule
import database.repositories.{ FileRepository, FileRepositoryImpl, TaskRepository, TaskRepositoryImpl }
import database.utils.DatabaseUtils._
import slick.jdbc.MySQLProfile.api._

class Module extends AbstractModule {

  def configure(): Unit = {
    bind(classOf[Database]).toInstance(DEFAULT_DB)
    bind(classOf[FileRepository]).toInstance(new FileRepositoryImpl(DEFAULT_DB))
    bind(classOf[TaskRepository]).toInstance(new TaskRepositoryImpl(DEFAULT_DB))
    bind(classOf[UUIDGenerator]).toInstance(new DefaultUUIDGenerator)
  }
}
