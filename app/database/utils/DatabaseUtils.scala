package database.utils
import javax.inject.Singleton
import slick.jdbc.H2Profile
import slick.jdbc.MySQLProfile.backend.Database

@Singleton
object DatabaseUtils {
  val DEFAULT_DB = Database.forConfig("dbinfo")
  val TEST_DB = H2Profile.api.Database.forURL(
    "jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
    driver = "org.h2.Driver")
}