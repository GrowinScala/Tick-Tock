package api.services

object DayType extends Enumeration{

  type DayType = String

  lazy val Weekday = "Weekday"
  lazy val Weekend = "Weekend"
}
