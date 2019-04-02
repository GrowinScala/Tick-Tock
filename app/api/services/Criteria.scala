package api.services

object Criteria extends Enumeration {

  type Criteria = String

  lazy val First: Criteria = "First"
  lazy val Second: Criteria = "Second"
  lazy val Third: Criteria = "Third"
  lazy val Fourth: Criteria = "Fourth"
  lazy val Last: Criteria = "Last"
}
