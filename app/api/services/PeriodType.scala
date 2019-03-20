package api.services

import play.api.mvc.QueryStringBindable
import api.validators.Error
import api.validators.Error._

/**
  *
  */
object PeriodType extends Enumeration{

  type PeriodType = String

  lazy val Minutely = "Minutely"
  lazy val Hourly = "Hourly"
  lazy val Daily = "Daily"
  lazy val Weekly = "Weekly"
  lazy val Monthly = "Monthly"
  lazy val Yearly = "Yearly"

  /*type PeriodType = Value
  val Minutely, Hourly, Daily, Weekly, Monthly, Yearly = Value

  implicit def periodTypeBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[PeriodType] = new QueryStringBindable[PeriodType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PeriodType]] = {
      if(key.equals("dayType")){
        text match {
          case "Minutely" => Some(Right(Minutely))
          case "Hourly" => Some(Right(Hourly))
          case "Daily" => Some(Right(Daily))
          case "Weekly" => Some(Right(Weekly))
          case "Monthly" => Some(Right(Monthly))
          case "Yearly" => Some(Right(Yearly))
          case _ => Some(Left(invalidPeriodType))
        }
      }
      else None
    }
    //override def unbind

  }*/

  /*implicit def periodTypeBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[PeriodType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, PeriodType]] = {
      for {
        index <- intBinder.bind(key + ".index", params)
        size <- intBinder.bind(key + ".size", params)
        } yield {
        (index, size) match {
          case (Right(index), Right(size)) => Right(Pager(index, size))
          case _ => Left("Unable to bind a Pager")
          }
        }
      }
    override def unbind(key: String, pager: Pager): String = {
      intBinder.unbind(key + ".index", pager.index) + "&" + intBinder.unbind(key + ".size", pager.size)
    }
  }*/


}

