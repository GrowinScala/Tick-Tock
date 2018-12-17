package database.utils
import slick.jdbc.MySQLProfile.api._

object DatabaseUtils {

  final val DEFAULT_DB = Database.forConfig("dbinfo")
  final val TEST_DB = Database.forURL("jdbc:h2:mem:play;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
    driver="org.h2.Driver")
}
