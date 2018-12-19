package database.utils
import slick.jdbc.{H2Profile, MySQLProfile}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.Database
import javax.inject.Singleton

@Singleton
class DatabaseUtils {

  val DEFAULT_DB = Database.forConfig("dbinfo")
  val TEST_DB = H2Profile.api.Database.forURL("jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
    driver="org.h2.Driver")
}
