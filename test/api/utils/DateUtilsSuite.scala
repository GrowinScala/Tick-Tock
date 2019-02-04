package api.utils

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import org.scalatest.{AsyncWordSpec, FlatSpec}
import api.utils.DateUtils._
import org.scalatestplus.play.PlaySpec

class DateUtilsSuite extends PlaySpec{

  val calendar = Calendar.getInstance()

  "DateUtils#stringToDateFormat" should {
    "convert a string in a certain format into a Date object. (yyyy-MM-dd HH:mm:ss format)" in {
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      stringToDateFormat("2019-01-01 12:00:00", "yyyy-MM-dd HH:mm:ss").toString mustBe "Tue Jan 01 12:00:00 GMT 2019"
    }

    "convert a string in a certain format into a Date object. (dd-MM-yyyy HH:mm:ss format)" in {
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      stringToDateFormat("01-02-2019 14:30:00", "dd-MM-yyyy HH:mm:ss").toString mustBe "Fri Feb 01 14:30:00 GMT 2019"
    }

    "convert a string in a certain format into a Date object. (yyyy/MM/dd HH:mm:ss format)" in {
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      stringToDateFormat("2020/06/15 16:15:45", "yyyy/MM/dd HH:mm:ss").toString mustBe "Mon Jun 15 16:15:45 BST 2020"
    }

    "convert a string in a certain format into a Date object. (dd/MM/yyyy HH:mm:ss format)" in {
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      stringToDateFormat("25/12/2021 20:30:40", "dd/MM/yyyy HH:mm:ss").toString mustBe "Sat Dec 25 20:30:40 GMT 2021"
    }
  }

  "DateUtils#dateToStringFormat" should {
    "convert a date to its string format in a certain format. (yyyy-MM-dd HH:mm:ss format)" in{
      calendar.set(2019, 1 - 1, 1, 12, 0, 0)
      val date = calendar.getTime
      dateToStringFormat(date, "yyyy-MM-dd HH:mm:ss") mustBe "2019-01-01 12:00:00"
    }

    "convert a date to its string format in a certain format. (dd-MM-yyyy HH:mm:ss format)" in{
      calendar.set(2019, 2 - 1, 1, 14, 30, 0)
      val date = calendar.getTime
      dateToStringFormat(date, "dd-MM-yyyy HH:mm:ss") mustBe "01-02-2019 14:30:00"
    }

    "convert a date to its string format in a certain format. (yyyy/MM/dd HH:mm:ss format)" in{
      calendar.set(2020, 6 - 1, 15, 16, 15, 45)
      val date = calendar.getTime
      dateToStringFormat(date, "yyyy/MM/dd HH:mm:ss") mustBe "2020/06/15 16:15:45"
    }

    "convert a date to its string format in a certain format. (dd/MM/yyyy HH:mm:ss format)" in{
      calendar.set(2021, 12 - 1, 25, 20, 30, 40)
      val date = calendar.getTime
      dateToStringFormat(date, "dd/MM/yyyy HH:mm:ss") mustBe "25/12/2021 20:30:40"
    }
  }

  "DateUtils#parseDate" should {
    "try to parse a correct date in string form and return it in Date form. (yyyy-MM-dd HH:mm:ss format)" in {
      parseDate("2019-01-01 12:00:00").get.toString mustBe "Tue Jan 01 12:00:00 GMT 2019"
    }

    "try to parse a correct date in string form and return it in Date form. (dd-MM-yyyy HH:mm:ss format)" in {
      parseDate("01-02-2019 14:30:00").get.toString mustBe "Fri Feb 01 14:30:00 GMT 2019"
    }

    "try to parse a correct date in string form and return it in Date form. (yyyy/MM/dd HH:mm:ss format)" in {
      parseDate("2020/06/15 16:15:45").get.toString mustBe "Mon Jun 15 16:15:45 BST 2020"
    }

    "try to parse a correct date in string form and return it in Date form. (dd/MM/yyyy HH:mm:ss format)" in {
      parseDate("25/12/2021 20:30:40").get.toString mustBe "Sat Dec 25 20:30:40 GMT 2021"
    }

    "try to parse an incorrect date in string form and return None." in {
      parseDate("123-456/789 123456789") mustBe None
    }
  }
}
