import com.google.inject.AbstractModule
import database.repositories.{FileRepository, TaskRepository}
import database.repositories.slick.{FileRepositoryImpl, TaskRepositoryImpl}
import database.utils.DatabaseUtils._

class Module extends AbstractModule {

  def configure = {
    Seq(bind(classOf[FileRepository]).toInstance(new FileRepositoryImpl(DEFAULT_DB)),
      bind(classOf[TaskRepository]).toInstance(new TaskRepositoryImpl(DEFAULT_DB)))
  }
}
