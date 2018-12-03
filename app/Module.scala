import com.google.inject.AbstractModule
import org.h2.engine.Database
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

class Module extends AbstractModule{

  def configure = {
    bind(classOf[DatabaseConfig[MySQLProfile]]).toInstance(DatabaseConfig.forConfig("dbinfo"))
  }


}
