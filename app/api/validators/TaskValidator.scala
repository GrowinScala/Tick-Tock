package api.validators

import java.util.{ Calendar, Date, TimeZone, UUID }

import api.dtos._
import api.services.{ Criteria, DayType, SchedulingType }
import api.utils.DateUtils._
import api.utils.UUIDGenerator
import api.validators.Error._
import database.repositories.{ FileRepository, TaskRepository }
import javax.inject.{ Inject, Singleton }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }
import scala.util.Try

/**
 * Object that handles the validation for the received JSON's on the HTTP request controller classes.
 */
@Singleton
class TaskValidator @Inject() (implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val UUIDGen: UUIDGenerator) {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val calendar: Calendar = Calendar.getInstance

  //---------------------------------------------------------
  //# TASK VALIDATORS
  //---------------------------------------------------------

  def scheduleValidator(task: CreateTaskDTO): Either[List[Error], TaskDTO] = {
    val taskId = UUIDGen.generateUUID
    val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
    val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
    val timezone = isValidTimezone(task.timezone)
    val exclusionDates = areValidExclusionDateFormats(task.exclusions, startDate, endDate)
    val exclusions = areValidExclusionFormats(task.exclusions, exclusionDates, taskId)
    val schedulingDates = areValidSchedulingDateFormats(task.schedulings, startDate, endDate)
    val schedulings = areValidSchedulingFormats(task.schedulings, schedulingDates, taskId)

    val errorList: List[(Boolean, Error)] = (List(
      (isValidCreateTask(task), invalidCreateTaskFormat),
      (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
      (isValidStartDateValue(startDate), invalidStartDateValue),
      (isValidFileName(Some(task.fileName)), invalidFileName),
      (isValidTaskType(Some(task.taskType)), invalidTaskType),
      (isValidPeriodType(task.periodType), invalidPeriodType),
      (isValidPeriod(task.period), invalidPeriod),
      (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
      (isValidEndDateValue(startDate, endDate), invalidEndDateValue),
      (isValidOccurrences(task.occurrences), invalidOccurrences),
      (task.timezone.isEmpty || timezone.isDefined, invalidTimezone),
      (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat),
      ((exclusionDates.isEmpty && !existsAtLeastOneExclusionDate(task.exclusions)) || exclusionDates.nonEmpty, invalidExclusionDateFormat),
      (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
      ((schedulingDates.isEmpty && !existsAtLeastOneSchedulingDate(task.schedulings)) || schedulingDates.nonEmpty, invalidSchedulingDateFormat))
      ::: areValidExclusions(exclusions, startDate, endDate)
      ::: areValidSchedulings(schedulings, startDate, endDate)).filter(errorList => errorList match {
        case (isValid, _) => !isValid
      })

    if (errorList.isEmpty) Right(TaskDTO(taskId, task.fileName, task.taskType, startDate, task.periodType, task.period, endDate, task.occurrences, task.occurrences, if (timezone.isDefined) Some(timezone.get.getID) else None, exclusions, schedulings))
    else Left(errorList.unzip match {
      case (_, errors) => errors
    })
  }

  def updateValidator(id: String, task: UpdateTaskDTO): Either[List[Error], TaskDTO] = {
    val oldDTO = Await.result(taskRepo.selectTask(id), 5 seconds)
    if (oldDTO.isDefined) {
      val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
      val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
      val timezone = isValidTimezone(task.timezone)
      val exclusionDates = areValidUpdateExclusionDateFormats(task.exclusions, startDate, endDate)
      val exclusions = areValidUpdateExclusionFormats(oldDTO.get.exclusions, task.exclusions, exclusionDates, id)
      val schedulingDates = areValidUpdateSchedulingDateFormats(task.schedulings, startDate, endDate)
      val schedulings = areValidUpdateSchedulingFormats(oldDTO.get.schedulings, task.schedulings, schedulingDates, id)

      val errorList: List[(Boolean, Error)] = (List(
        (isValidUpdateTask(task, oldDTO.get), invalidUpdateTaskFormat),
        (isValidUUID(task.taskId), invalidTaskUUID),
        (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
        (isValidStartDateValue(startDate), invalidStartDateValue),
        (isValidFileName(task.fileName), invalidFileName),
        (isValidTaskType(task.taskType) || task.taskType.isEmpty, invalidTaskType),
        (isValidPeriodType(task.periodType), invalidPeriodType),
        (isValidPeriod(task.period), invalidPeriod),
        (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
        (isValidOccurrences(task.occurrences), invalidOccurrences),
        (task.timezone.isEmpty || timezone.isDefined, invalidTimezone),
        (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat),
        ((exclusionDates.isEmpty && !existsAtLeastOneUpdateExclusionDate(task.exclusions)) || exclusionDates.isDefined, invalidExclusionDateFormat),
        (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
        ((schedulingDates.isEmpty && !existsAtLeastOneUpdateSchedulingDate(task.schedulings)) || schedulingDates.isDefined, invalidSchedulingDateFormat))
        ::: areValidExclusions(exclusions, startDate, endDate)
        ::: areValidSchedulings(schedulings, startDate, endDate)).filter(!_._1)

      val oldStartDate = if (oldDTO.get.startDateAndTime.isDefined) oldDTO.get.startDateAndTime else None
      val oldPeriodType = if (oldDTO.get.periodType.isDefined) oldDTO.get.periodType else None
      val oldPeriod = if (oldDTO.get.period.isDefined) oldDTO.get.period else None
      val oldEndDate = if (oldDTO.get.endDateAndTime.isDefined) oldDTO.get.endDateAndTime else None
      val oldTotalOccurrences = if (oldDTO.get.totalOccurrences.isDefined) oldDTO.get.totalOccurrences else None
      val oldCurrentOccurrences = if (oldDTO.get.currentOccurrences.isDefined) oldDTO.get.currentOccurrences else None
      val oldTimezone = if (oldDTO.get.timezone.isDefined) oldDTO.get.timezone else None
      val oldExclusions = if (oldDTO.get.exclusions.isDefined) oldDTO.get.exclusions else None
      val oldSchedulings = if (oldDTO.get.schedulings.isDefined) oldDTO.get.schedulings else None

      if (errorList.isEmpty) {
        Right(TaskDTO(
          task.taskId.getOrElse(oldDTO.get.taskId), //taskId
          task.fileName.getOrElse(oldDTO.get.fileName), //fileName
          task.taskType.getOrElse(oldDTO.get.taskType), //taskType
          if (startDate.isDefined) startDate else oldStartDate, //startDate
          if (task.periodType.isDefined) task.periodType else oldPeriodType, //periodType
          if (task.period.isDefined) task.period else oldPeriod, //period
          if (task.occurrences.isDefined && oldEndDate.isDefined) None else if (endDate.isDefined) endDate else oldEndDate, //endDate
          if (endDate.isDefined && oldTotalOccurrences.isDefined) None else if (task.occurrences.isDefined) task.occurrences else oldTotalOccurrences, //totalOccurrences
          if (endDate.isDefined && oldCurrentOccurrences.isDefined) None else if (task.occurrences.isDefined) task.occurrences else oldCurrentOccurrences, //currentOccurrences
          if (task.timezone.isDefined) Some(timezone.get.getID) else oldTimezone, //timezone
          if (task.exclusions.isDefined) exclusions else oldExclusions, //exclusions
          if (task.schedulings.isDefined) schedulings else oldSchedulings //schedulings
        ))
      } else Left(errorList.unzip match {
        case (_, errors) => errors
      })
    } else Left(List(invalidEndpointId))
  }

  private def isValidCreateTask(task: CreateTaskDTO): Boolean = {
    task.taskType match {
      case SchedulingType.RunOnce =>
        task.periodType.isEmpty &&
          task.period.isEmpty &&
          task.endDateAndTime.isEmpty &&
          task.occurrences.isEmpty &&
          task.exclusions.isEmpty &&
          task.schedulings.isEmpty
      case SchedulingType.Periodic =>
        task.periodType.isDefined && task.period.isDefined && task.schedulings.isEmpty &&
          ((task.endDateAndTime.isDefined && task.occurrences.isEmpty) || (task.endDateAndTime.isEmpty && task.occurrences.isDefined))
      case SchedulingType.Personalized =>
        task.periodType.isEmpty && task.period.isEmpty && task.schedulings.isDefined &&
          ((task.endDateAndTime.isDefined && task.occurrences.isEmpty) || (task.endDateAndTime.isEmpty && task.occurrences.isDefined))
      case _ => false
    }
  }

  private def isValidUpdateTask(task: UpdateTaskDTO, oldTask: TaskDTO): Boolean = {
    if (task.taskType.isDefined && task.taskType.get.equals("Periodic")) {
      (task.startDateAndTime.isDefined || oldTask.startDateAndTime.isDefined) &&
        (task.periodType.isDefined || oldTask.periodType.isDefined) &&
        (task.period.isDefined || oldTask.period.isDefined) &&
        (task.endDateAndTime.isDefined || oldTask.endDateAndTime.isDefined || task.occurrences.isDefined || oldTask.totalOccurrences.isDefined)
    } else {
      (task.taskId.isDefined || task.fileName.isDefined || task.taskType.isDefined || task.startDateAndTime.isDefined ||
        task.periodType.isDefined || task.period.isDefined || task.endDateAndTime.isDefined || task.occurrences.isDefined) &&
        !(task.endDateAndTime.isDefined && task.occurrences.isDefined)
    }

  }

  private def isValidUUID(uuid: Option[String]): Boolean = {
    if (uuid.isDefined) {
      val parsedUUID = Try(Some(UUID.fromString(uuid.get))).getOrElse(None)
      parsedUUID.isDefined
    } else true
  }

  private def isValidStartDateFormat(startDate: Option[String], timezone: Option[String]): Option[Date] = {
    if (startDate.isDefined) {
      if (timezone.isDefined && isValidTimezone(timezone).isDefined) parseDateWithTimezone(startDate.get, timezone.get)
      else parseDate(startDate.get)
    } else None
  }

  /**
   * Checks if the date given is valid, (if it already happened or not)
   *
   * @param date The Date to be checked
   * @return Returns a ValidationError if its not valid. None otherwise.
   */
  private def isValidStartDateValue(startDate: Option[Date]): Boolean = {
    if (startDate.isDefined) startDate.get.after(getCurrentDate)
    else true
  }

  /**
   * Checks if the file with the given fileName exists.
   *
   * @param fileName The fileName to be checked.
   * @return Returns a ValidationError if its not valid. None otherwise.
   */
  private def isValidFileName(fileName: Option[String]): Boolean = {
    if (fileName.isDefined) Await.result(fileRepo.existsCorrespondingFileName(fileName.get), Duration.Inf)
    else true
  }

  private def isValidTaskType(taskType: Option[String]): Boolean = {
    if (taskType.isDefined) taskType.get.equals("RunOnce") || taskType.get.equals("Periodic") || taskType.get.equals("Personalized")
    else false
  }

  private def isValidPeriodType(periodType: Option[String]): Boolean = {
    periodType.isEmpty ||
      periodType.get.equals("Minutely") ||
      periodType.get.equals("Hourly") ||
      periodType.get.equals("Daily") ||
      periodType.get.equals("Weekly") ||
      periodType.get.equals("Monthly") ||
      periodType.get.equals("Yearly")
  }

  private def isValidPeriod(period: Option[Int]): Boolean = {
    period.isEmpty || period.get > 0
  }

  private def isValidEndDateFormat(endDate: Option[String], timezone: Option[String]): Option[Date] = {
    if (endDate.isDefined) {
      if (timezone.isDefined && isValidTimezone(timezone).isDefined) parseDateWithTimezone(endDate.get, timezone.get)
      else parseDate(endDate.get)
    } else None
  }

  private def isValidEndDateValue(startDate: Option[Date], endDate: Option[Date]): Boolean = {
    if (startDate.isDefined && endDate.isDefined) endDate.isEmpty || (endDate.get.after(startDate.get) && endDate.get.after(getCurrentDate))
    else true
  }

  private def isValidOccurrences(occurrences: Option[Int]): Boolean = {
    occurrences.isEmpty || occurrences.get > 0
  }

  private def isValidTimezone(timezone: Option[String]): Option[TimeZone] = {
    if (timezone.isDefined) {
      val parsedTimezone = parseTimezone(timezone.get)
      if (parsedTimezone.isDefined) parsedTimezone
      else None
    } else None
  }

  private def areValidUpdateExclusionFormats(oldExclusions: Option[List[ExclusionDTO]], exclusions: Option[List[UpdateExclusionDTO]], exclusionDates: Option[List[Option[Date]]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(exclusions: List[UpdateExclusionDTO], exclusionDates: List[Option[Date]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if (exclusions.isEmpty) None
      else {
        val exclusion = exclusions.head
        val oldExclusion = getOldExclusionWithExclusionId(exclusion.exclusionId, oldExclusions)
        if (oldExclusion.isDefined) {
          exclusion.exclusionDate match {
            case Some(_) =>
              val exclusionDate = exclusionDates.head
              if (exclusionDate.isDefined && exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty &&
                exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty && exclusion.criteria.isEmpty) {
                iter(exclusions.tail, exclusionDates.tail, ExclusionDTO(exclusion.exclusionId.get, taskId, exclusionDate) :: toReturn)
              } else None
            case None =>
              if (exclusion.taskId.isDefined || exclusion.exclusionDate.isDefined || exclusion.day.isDefined
                || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined || exclusion.month.isDefined || exclusion.year.isDefined
                || exclusion.criteria.isDefined) {
                iter(exclusions.tail, exclusionDates.tail, ExclusionDTO(exclusion.exclusionId.get, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn)
              } else None
          }
        } else None
      }
    }
    exclusions match {
      case Some(exclusionList) => iter(exclusionList, exclusionDates.get, Nil)
      case None => None
    }
  }

  private def areValidUpdateExclusionDateFormats(exclusions: Option[List[UpdateExclusionDTO]], startDate: Option[Date], endDate: Option[Date]): Option[List[Option[Date]]] = {
    def iter(list: List[UpdateExclusionDTO], toReturn: List[Option[Date]]): Option[List[Option[Date]]] = {
      if (list.nonEmpty) {
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if (parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else None
          case None => iter(list.tail, None :: toReturn)
        }
      } else Some(toReturn)
    }
    if (exclusions.isDefined) iter(exclusions.get, Nil)
    else None
  }

  private def getOldExclusionWithExclusionId(exclusionId: Option[String], oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
    def iter(oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
      if ((oldExclusions.isEmpty && oldExclusions.get.isEmpty) || exclusionId.isEmpty) None
      else if (oldExclusions.get.head.exclusionId.equals(exclusionId.get)) Some(oldExclusions.get.head) else iter(Some(oldExclusions.get.tail))
    }
    iter(oldExclusions)
  }

  private def areValidExclusions(exclusions: Option[List[ExclusionDTO]], startDate: Option[Date], endDate: Option[Date]): List[(Boolean, Error)] = {
    if (exclusions.isDefined) {
      if (startDate.isDefined) calendar.setTime(startDate.get) else calendar.setTime(new Date())
      List(
        (areValidExclusionDateValues(exclusions, endDate), invalidExclusionDateValue),
        (areValidExclusionDayValues(exclusions), invalidExclusionDayValue),
        (areValidExclusionDayOfWeekValues(exclusions), invalidExclusionDayOfWeekValue),
        (areValidExclusionDayTypeValues(exclusions), invalidExclusionDayTypeValue),
        (areValidExclusionMonthValues(exclusions), invalidExclusionMonthValue),
        (areValidExclusionYearValues(exclusions), invalidExclusionYearValue),
        (areValidExclusionCriteriaValues(exclusions), invalidExclusionCriteriaValue))
    } else Nil

  }

  private def existsAtLeastOneExclusionDate(exclusions: Option[List[CreateExclusionDTO]]): Boolean = {
    if (exclusions.isDefined) {
      exclusions.get.foreach(elem => if (elem.exclusionDate.isEmpty) false)
      true
    } else false
  }

  private def existsAtLeastOneUpdateExclusionDate(exclusions: Option[List[UpdateExclusionDTO]]): Boolean = {
    if (exclusions.isDefined) {
      exclusions.get.foreach(elem => if (elem.exclusionDate.isEmpty) false)
      true
    } else false
  }

  private def areValidExclusionFormats(exclusions: Option[List[CreateExclusionDTO]], exclusionDates: List[Option[Date]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(list: List[CreateExclusionDTO], dateList: List[Option[Date]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if (list.isEmpty) Some(toReturn)
      else {
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(_) =>
            val exclusionDate = Try(dateList.head).getOrElse(None)
            if (exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty && exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty)
              iter(list.tail, Try(dateList.tail).getOrElse(Nil), ExclusionDTO(UUIDGen.generateUUID, taskId, exclusionDate) :: toReturn)
            else None
          case None =>
            if (exclusion.day.isDefined || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined || exclusion.month.isDefined || exclusion.year.isDefined)
              iter(list.tail, Try(dateList.tail).getOrElse(Nil), ExclusionDTO(UUIDGen.generateUUID, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn)
            else None
        }
      }
    }
    exclusions match {
      case Some(exclusionList) => iter(exclusionList, exclusionDates, Nil)
      case None => None
    }
  }

  private def areValidExclusionDateFormats(exclusions: Option[List[CreateExclusionDTO]], startDate: Option[Date], endDate: Option[Date]): List[Option[Date]] = {
    def iter(list: List[CreateExclusionDTO], toReturn: List[Option[Date]]): List[Option[Date]] = {
      if (list.nonEmpty) {
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if (parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else List()
          case None => iter(list.tail, None :: toReturn)
        }
      } else toReturn
    }
    if (exclusions.isDefined) iter(exclusions.get, Nil)
    else List()
  }

  private def areValidExclusionDateValues(exclusions: Option[List[ExclusionDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.exclusionDate.isDefined) {
          if (exclusion.exclusionDate.get.after(calendar.getTime)) {
            if (endDate.isDefined) if (endDate.get.after(exclusion.exclusionDate.get)) iter(list.tail) else false
            else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }

  }

  private def areValidExclusionDayValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.day.isDefined) {
          if (exclusion.day.get >= 1 && exclusion.day.get <= 31) {
            if (exclusion.month.isDefined) {
              exclusion.day.get match {
                case 29 => exclusion.month.get != 2 || (exclusion.year match {
                  case Some(year) => isLeapYear(year)
                  case None => false
                })
                case 30 => exclusion.month.get != 2
                case 31 => exclusion.month.get != 2 && exclusion.month.get != 4 && exclusion.month.get != 6 && exclusion.month.get != 9 && exclusion.month.get != 11
              }
              if (exclusion.year.isDefined) {
                if (exclusion.month.get == calendar.get(Calendar.MONTH) && exclusion.year.get == calendar.get(Calendar.YEAR)) if (exclusion.day.get >= calendar.get(Calendar.DAY_OF_MONTH)) iter(list.tail) else false
                else if (exclusion.year.get >= calendar.get(Calendar.YEAR) && exclusion.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              } else iter(list.tail)
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true

    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }

  }

  private def areValidExclusionDayOfWeekValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.dayOfWeek.isDefined) {
          if (exclusion.dayOfWeek.get >= 1 && exclusion.dayOfWeek.get <= 7) {
            if (exclusion.dayType.isDefined) {
              if (exclusion.dayOfWeek.get >= 2 && exclusion.dayOfWeek.get <= 6) if (exclusion.dayType.get == DayType.Weekday) iter(list.tail) else false
              else if (exclusion.dayType.get == DayType.Weekend) iter(list.tail) else false
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidExclusionDayTypeValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        exclusion.dayType match {
          case Some(DayType.Weekday) =>
            exclusion.dayOfWeek match {
              case Some(value) =>
                if ((1 to 5).contains(value)) iter(list.tail)
                else false
              case None => iter(list.tail)
            }
          case Some(DayType.Weekend) =>
            exclusion.dayOfWeek match {
              case Some(value) =>
                if ((6 to 7).contains(value)) iter(list.tail)
                else false
              case None => iter(list.tail)
            }
          case Some(_) => false
          case None => iter(list.tail)
        }
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  /*private def areValidExclusionDayTypeValues(exclusions: List[ExclusionDTO]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.dayType.isDefined) {
          if (exclusion.dayType.get == DayType.Weekday) if (exclusion.dayOfWeek.get >= 1 && exclusion.dayOfWeek.get <= 5) iter(list.tail) else false
          else if (exclusion.dayType.get == DayType.Weekend) if (exclusion.dayOfWeek.get == 6 || exclusion.dayOfWeek.get == 7) iter(list.tail) else false
          else false
        }
        else iter(list.tail)
      }
      else true
    }
    iter(exclusions)
  }*/

  private def areValidExclusionMonthValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.month.isDefined) {
          if (exclusion.month.get >= 1 && exclusion.month.get <= 12) {
            if (exclusion.year.isDefined) {
              if (exclusion.year.get == calendar.get(Calendar.YEAR)) if (exclusion.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if (exclusion.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidExclusionYearValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.year.isDefined) if (exclusion.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
        else iter(list.tail)
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidExclusionCriteriaValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.criteria.isDefined) if (exclusion.criteria.get == Criteria.First || exclusion.criteria.get == Criteria.Second || exclusion.criteria.get == Criteria.Third ||
          exclusion.criteria.get == Criteria.Fourth || exclusion.criteria.get == Criteria.Last) iter(list.tail) else false
        else iter(list.tail)
      } else true
    }
    exclusions match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def isValidSchedulingFormat(schedulings: Option[List[CreateSchedulingDTO]], schedulingDates: Option[List[Option[Date]]], taskId: String): Option[List[SchedulingDTO]] = {
    def iter(list: List[CreateSchedulingDTO], dateList: Option[List[Option[Date]]], toReturn: List[SchedulingDTO]): Option[List[SchedulingDTO]] = {
      if (list.isEmpty) Some(toReturn)
      else {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(_) =>
            val schedulingDate = schedulingDates.get.head
            if (scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty && scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty)
              iter(list.tail, Some(dateList.get.tail), SchedulingDTO(UUIDGen.generateUUID, taskId, schedulingDate) :: toReturn)
            else None
          case None =>
            if (scheduling.day.isDefined || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined || scheduling.month.isDefined || scheduling.year.isDefined)
              iter(list.tail, None, SchedulingDTO(UUIDGen.generateUUID, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn)
            else None
        }
      }
    }
    schedulings match {
      case Some(scheduleList) => iter(scheduleList, schedulingDates, Nil)
      case None => None
    }
  }

  /*private def isValidSchedulingFormat(schedulings: Option[List[CreateSchedulingDTO]], taskId: String): Option[List[SchedulingDTO]] = {
    val toReturn: List[SchedulingDTO] = Nil
    if (schedulings.isDefined) {
      schedulings.get.foreach { scheduling =>
        if (scheduling.schedulingDate.isDefined) {
          val parsedDate = parseDate(scheduling.schedulingDate.get)
          if (parsedDate.isDefined && scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty &&
            scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty && scheduling.criteria.isEmpty)
            SchedulingDTO(UUIDGen.generateUUID, taskId, parsedDate, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn

        }
        else {
          if ((scheduling.day.isDefined || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined ||
            scheduling.month.isDefined || scheduling.year.isDefined) &&
            (scheduling.criteria.isEmpty || scheduling.criteria.get == Criteria.First || scheduling.criteria.get == Criteria.Second ||
              scheduling.criteria.get == Criteria.Third || scheduling.criteria.get == Criteria.Fourth || scheduling.criteria.get == Criteria.Last))
            SchedulingDTO(UUIDGen.generateUUID, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn
        }
      }
      if (toReturn.size == schedulings.get.size) Some(toReturn)
      else None
    }
    else None
  }*/

  private def areValidUpdateSchedulingFormats(oldSchedulings: Option[List[SchedulingDTO]], schedulings: Option[List[UpdateSchedulingDTO]], schedulingDates: Option[List[Option[Date]]], taskId: String): Option[List[SchedulingDTO]] = {
    def iter(schedulings: List[UpdateSchedulingDTO], schedulingDates: List[Option[Date]], toReturn: List[SchedulingDTO]): Option[List[SchedulingDTO]] = {
      if (schedulings.isEmpty) None
      else {
        val scheduling = schedulings.head
        val oldExclusion = getOldSchedulingWithSchedulingId(scheduling.schedulingId, oldSchedulings)
        if (oldExclusion.isDefined) {
          scheduling.schedulingDate match {
            case Some(_) =>
              val exclusionDate = schedulingDates.head
              if (exclusionDate.isDefined && scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty &&
                scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty && scheduling.criteria.isEmpty) {
                iter(schedulings.tail, schedulingDates.tail, SchedulingDTO(scheduling.schedulingId.get, taskId, exclusionDate) :: toReturn)
              } else None
            case None =>
              if (scheduling.taskId.isDefined || scheduling.schedulingDate.isDefined || scheduling.day.isDefined
                || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined || scheduling.month.isDefined || scheduling.year.isDefined
                || scheduling.criteria.isDefined) {
                iter(schedulings.tail, schedulingDates.tail, SchedulingDTO(scheduling.schedulingId.get, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn)
              } else None
          }
        } else None
      }
    }
    schedulings match {
      case Some(schedulingList) => iter(schedulingList, schedulingDates.get, Nil)
      case None => None
    }
  }

  private def areValidUpdateSchedulingDateFormats(schedulings: Option[List[UpdateSchedulingDTO]], startDate: Option[Date], endDate: Option[Date]): Option[List[Option[Date]]] = {
    def iter(list: List[UpdateSchedulingDTO], toReturn: List[Option[Date]]): Option[List[Option[Date]]] = {
      if (list.nonEmpty) {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if (parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else None
          case None => iter(list.tail, None :: toReturn)
        }
      } else Some(toReturn)
    }
    if (schedulings.isDefined) iter(schedulings.get, Nil)
    else None
  }

  /*private def areValidUpdateExclusionFormats(oldExclusions: Option[List[ExclusionDTO]], exclusions: Option[List[UpdateExclusionDTO]], exclusionDates: Option[List[Option[Date]]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(exclusions: List[UpdateExclusionDTO], exclusionDates: List[Option[Date]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if(exclusions.isEmpty) None
      else{
        val exclusion = exclusions.head
        val oldExclusion = getOldExclusionWithExclusionId(exclusion.exclusionId, oldExclusions)
        if(oldExclusion.isDefined) {
          exclusion.exclusionDate match {
            case Some(_) =>
              val exclusionDate = exclusionDates.head
              if (exclusionDate.isDefined && exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty &&
                exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty && exclusion.criteria.isEmpty) {
                iter(exclusions.tail, exclusionDates.tail, ExclusionDTO(exclusion.exclusionId.get, taskId, exclusionDate) :: toReturn)
              }
              else None
            case None =>
              if (exclusion.taskId.isDefined || exclusion.exclusionDate.isDefined || exclusion.day.isDefined
                || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined || exclusion.month.isDefined || exclusion.year.isDefined
                || exclusion.criteria.isDefined) {
                iter(exclusions.tail, exclusionDates.tail, ExclusionDTO(exclusion.exclusionId.get, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn)
              }
              else None
          }
        }
        else None
      }
    }
    exclusions match {
      case Some(exclusionList) => iter(exclusionList, exclusionDates.get, Nil)
      case None => None
    }
  }

  private def areValidUpdateExclusionDateFormats(exclusions: Option[List[UpdateExclusionDTO]], startDate: Option[Date], endDate: Option[Date]): Option[List[Option[Date]]] = {
    def iter(list: List[UpdateExclusionDTO], toReturn: List[Option[Date]]): Option[List[Option[Date]]] = {
      if(list.nonEmpty) {
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if(parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else None
          case None => iter(list.tail, None :: toReturn)
        }
      }
      else Some(toReturn)
    }
    if(exclusions.isDefined) iter(exclusions.get, Nil)
    else None
  }*/

  private def getOldSchedulingWithSchedulingId(schedulingId: Option[String], oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
    def iter(oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
      if ((oldSchedulings.isEmpty && oldSchedulings.get.isEmpty) || schedulingId.isEmpty) None
      else if (oldSchedulings.get.head.schedulingId.equals(schedulingId.get)) Some(oldSchedulings.get.head) else iter(Some(oldSchedulings.get.tail))
    }
    iter(oldSchedulings)
  }

  private def areValidSchedulings(schedulings: Option[List[SchedulingDTO]], startDate: Option[Date], endDate: Option[Date]): List[(Boolean, Error)] = {
    if (schedulings.isDefined) {
      if (startDate.isDefined) calendar.setTime(startDate.get) else calendar.setTime(new Date())
      List(
        (areValidSchedulingDateValues(schedulings, endDate), invalidSchedulingDateValue),
        (areValidSchedulingDayValues(schedulings), invalidSchedulingDayValue),
        (areValidSchedulingDayOfWeekValues(schedulings), invalidSchedulingDayOfWeekValue),
        (areValidSchedulingDayTypeValues(schedulings), invalidSchedulingDayTypeValue),
        (areValidSchedulingMonthValues(schedulings), invalidSchedulingMonthValue),
        (areValidSchedulingYearValues(schedulings), invalidSchedulingYearValue),
        (areValidSchedulingCriteriaValues(schedulings), invalidSchedulingCriteriaValue))
    } else Nil
  }

  private def existsAtLeastOneSchedulingDate(schedulings: Option[List[CreateSchedulingDTO]]): Boolean = {
    if (schedulings.isDefined) {
      schedulings.get.foreach(elem => if (elem.schedulingDate.isEmpty) false)
      true
    } else false
  }

  private def existsAtLeastOneUpdateSchedulingDate(schedulings: Option[List[UpdateSchedulingDTO]]): Boolean = {
    if (schedulings.isDefined) {
      schedulings.get.foreach(elem => if (elem.schedulingDate.isEmpty) false)
      true
    } else false
  }

  /*private def areValidExclusions(exclusions: Option[List[CreateExclusionDTO]], taskId: String, startDate: Option[Date], endDate: Option[Date]): List[(Boolean, Error)] = {
    val exclusionDates = areValidExclusionDateFormats(exclusions, startDate, endDate)
    val parsedExclusions = areValidExclusionFormats(exclusions, exclusionDates, taskId)
    if (exclusions.isDefined){
      if (startDate.isDefined) calendar.setTime(startDate.get) else calendar.setTime(new Date())
      List(
        (exclusions.isEmpty || parsedExclusions.isDefined, invalidExclusionFormat),
        ((exclusionDates.isEmpty && existsAtLeastOneExclusionDate(exclusions)) || exclusionDates.isDefined, invalidExclusionDateFormat),
        (areValidExclusionDateValues(parsedExclusions), invalidExclusionDateValue),
        (areValidExclusionDayValues(parsedExclusions), invalidExclusionDayValue),
        (areValidExclusionDayOfWeekValues(parsedExclusions), invalidExclusionDayOfWeekValue),
        (areValidExclusionDayTypeValues(parsedExclusions), invalidExclusionDayTypeValue),
        (areValidExclusionMonthValues(parsedExclusions), invalidExclusionMonthValue),
        (areValidExclusionYearValues(parsedExclusions), invalidExclusionYearValue),
        (areValidExclusionCriteriaValues(parsedExclusions), invalidExclusionCriteriaValue)
      )
    }
    else Nil

  }

  private def existsAtLeastOneExclusionDate(exclusions: Option[List[CreateExclusionDTO]]): Boolean = {
    if(exclusions.isDefined){
      exclusions.get.foreach(elem => if(elem.exclusionDate.isEmpty) false)
      true
    }
    else false
  }

  private def areValidExclusionFormats(exclusions: Option[List[CreateExclusionDTO]], exclusionDates: Option[List[Option[Date]]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(list: List[CreateExclusionDTO], dateList: Option[List[Option[Date]]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if(list.isEmpty) Some(toReturn)
      else{
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(_) =>
            val exclusionDate = exclusionDates.get.head
            if(exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty && exclusion.dayType.isEmpty && exclusion.month.isEmpty && exclusion.year.isEmpty)
              iter(list.tail, Some(dateList.get.tail), ExclusionDTO(UUIDGen.generateUUID, taskId, exclusionDate) :: toReturn)
            else None
          case None =>
            if(exclusion.day.isDefined || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined || exclusion.month.isDefined || exclusion.year.isDefined)
              iter(list.tail, None, ExclusionDTO(UUIDGen.generateUUID, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn)
            else None
        }
      }
    }
    exclusions match {
      case Some(exclusionList) => iter(exclusionList, exclusionDates, Nil)
      case None => None
    }
  }*/

  private def areValidSchedulingFormats(schedulings: Option[List[CreateSchedulingDTO]], schedulingDates: List[Option[Date]], taskId: String): Option[List[SchedulingDTO]] = {
    def iter(list: List[CreateSchedulingDTO], dateList: List[Option[Date]], toReturn: List[SchedulingDTO]): Option[List[SchedulingDTO]] = {
      if (list.isEmpty) Some(toReturn)
      else {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(_) =>
            val schedulingDate = Try(schedulingDates.head).getOrElse(None)
            if (scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty && scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty)
              iter(list.tail, Try(dateList.tail).getOrElse(Nil), SchedulingDTO(UUIDGen.generateUUID, taskId, schedulingDate) :: toReturn)
            else None
          case None =>
            if (scheduling.day.isDefined || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined || scheduling.month.isDefined || scheduling.year.isDefined)
              iter(list.tail, Try(dateList.tail).getOrElse(Nil), SchedulingDTO(UUIDGen.generateUUID, taskId, None, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn)
            else None
        }
      }
    }
    schedulings match {
      case Some(schedulingList) => iter(schedulingList, schedulingDates, Nil)
      case None => None
    }
  }

  private def areValidSchedulingDateFormats(schedulings: Option[List[CreateSchedulingDTO]], startDate: Option[Date], endDate: Option[Date]): List[Option[Date]] = {
    def iter(list: List[CreateSchedulingDTO], toReturn: List[Option[Date]]): List[Option[Date]] = {
      if (list.nonEmpty) {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if (parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else List()
          case None => iter(list.tail, None :: toReturn)
        }
      } else toReturn
    }
    if (schedulings.isDefined) iter(schedulings.get, Nil)
    else List()
  }

  private def areValidSchedulingDateValues(schedulings: Option[List[SchedulingDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.schedulingDate.isDefined) {
          if (scheduling.schedulingDate.get.after(calendar.getTime)) {
            if (endDate.isDefined) if (endDate.get.after(scheduling.schedulingDate.get)) iter(list.tail) else false
            else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingDayValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.day.isDefined) {
          if (scheduling.day.get >= 1 && scheduling.day.get <= 31) {
            if (scheduling.month.isDefined) {
              scheduling.day.get match {
                case 29 => {
                  scheduling.month.get != 2 || scheduling.year.isEmpty || isLeapYear(scheduling.year.get)
                }
                case 30 => {
                  scheduling.month.get != 2
                }
                case 31 => {
                  scheduling.month.get != 2 && scheduling.month.get != 4 && scheduling.month.get != 6 &&
                    scheduling.month.get != 9 && scheduling.month.get != 11
                }
              }
              if (scheduling.year.isDefined) {
                if (scheduling.month.get == calendar.get(Calendar.MONTH) && scheduling.year.get == calendar.get(Calendar.YEAR)) if (scheduling.day.get >= calendar.get(Calendar.DAY_OF_MONTH)) iter(list.tail) else false
                else if (scheduling.year.get >= calendar.get(Calendar.YEAR) && scheduling.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              } else iter(list.tail)
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true

    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingDayOfWeekValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.dayOfWeek.isDefined) {
          if (scheduling.dayOfWeek.get >= 1 && scheduling.dayOfWeek.get <= 7) {
            if (scheduling.dayType.isDefined) {
              if (scheduling.dayOfWeek.get >= 2 && scheduling.dayOfWeek.get <= 6) if (scheduling.dayType.get == DayType.Weekday) iter(list.tail) else false
              else if (scheduling.dayType.get == DayType.Weekend) iter(list.tail) else false
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingDayTypeValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        scheduling.dayType match {
          case Some(DayType.Weekday) =>
            scheduling.dayOfWeek match {
              case Some(dayOfWeek) => if (dayOfWeek >= 2 && dayOfWeek <= 6) iter(list.tail) else false
              case None => iter(list.tail)
            }
          case Some(DayType.Weekend) =>
            scheduling.dayOfWeek match {
              case Some(dayOfWeek) => if (dayOfWeek == 1 || dayOfWeek == 7) iter(list.tail) else false
              case None => iter(list.tail)
            }
          case Some(_) => false
          case None => iter(list.tail)
        }
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingMonthValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.month.isDefined) {
          if (scheduling.month.get >= 1 && scheduling.month.get <= 12) {
            if (scheduling.year.isDefined) {
              if (scheduling.year.get == calendar.get(Calendar.YEAR)) if (scheduling.month.get >= calendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if (scheduling.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
            } else iter(list.tail)
          } else false
        } else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingYearValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.year.isDefined) if (scheduling.year.get >= calendar.get(Calendar.YEAR)) iter(list.tail) else false
        else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

  private def areValidSchedulingCriteriaValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.criteria.isDefined) if (scheduling.criteria.get == Criteria.First || scheduling.criteria.get == Criteria.Second || scheduling.criteria.get == Criteria.Third ||
          scheduling.criteria.get == Criteria.Fourth || scheduling.criteria.get == Criteria.Last) iter(list.tail) else false
        else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

}
