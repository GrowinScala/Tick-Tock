import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import database.repositories.{FileRepository, FileRepositoryImpl, TaskRepository, TaskRepositoryImpl}
import database.utils.DatabaseUtils._

class Module extends AbstractModule {

  def configure = {
    Seq(bind(classOf[FileRepository]).toInstance(new FileRepositoryImpl(DEFAULT_DB)),
      bind(classOf[TaskRepository]).toInstance(new TaskRepositoryImpl(DEFAULT_DB)))
  }
}
