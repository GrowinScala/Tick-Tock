package api.validators

import java.util.{ Calendar, Date, TimeZone, UUID }

import api.dtos._
import api.services.Criteria.criteriaList
import api.services.PeriodType.periodTypeList
import api.services.{ DayType, SchedulingType }
import api.utils.DateUtils._
import api.utils.UUIDGenerator
import api.validators.Error._
import database.repositories.file.FileRepository
import database.repositories.task.TaskRepository
import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Object that handles the validation for the received JSON's on the HTTP request controller classes.
 */
@Singleton
class TaskValidator @Inject() (implicit val fileRepo: FileRepository, implicit val taskRepo: TaskRepository, implicit val UUIDGen: UUIDGenerator) {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val startCalendar: Calendar = Calendar.getInstance
  val endCalendar: Calendar = Calendar.getInstance

  //---------------------------------------------------------
  //# TASK VALIDATORS
  //---------------------------------------------------------

  def scheduleValidator(task: CreateTaskDTO): Future[Either[List[Error], TaskDTO]] = {
    val taskId = UUIDGen.generateUUID
    val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
    val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
    val timezone = isValidTimezone(task.timezone)
    val timezoneString = if (timezone.isDefined) Some(timezone.get.getID) else None
    val exclusionDates = areValidExclusionDateFormats(task.exclusions, startDate, endDate)
    val exclusions = areValidExclusionFormats(task.exclusions, exclusionDates, taskId)
    val schedulingDates = areValidSchedulingDateFormats(task.schedulings, startDate, endDate)
    val schedulings = areValidSchedulingFormats(task.schedulings, schedulingDates, taskId)

    val errorList: Future[List[(Boolean, Error)]] = isValidFileName(Some(task.fileName)).map { validFileName =>

      (List(
        (isValidCreateTask(task), invalidCreateTaskFormat),
        (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
        (isValidStartDateValue(startDate), invalidStartDateValue),
        (validFileName, invalidFileName),
        (isValidTaskType(Some(task.taskType)), invalidTaskType),
        (isValidPeriodType(task.periodType), invalidPeriodType),
        (isValidPeriod(task.period), invalidPeriod),
        (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
        (isValidEndDateValue(startDate, endDate), invalidEndDateValue),
        (isValidOccurrences(task.occurrences), invalidOccurrences),
        (task.timezone.isEmpty || timezoneString.isDefined, invalidTimezone),
        (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat),
        ((exclusionDates.isEmpty && !existsAtLeastOneExclusionDate(task.exclusions)) || exclusionDates.nonEmpty, invalidExclusionDateFormat),
        (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
        ((schedulingDates.isEmpty && !existsAtLeastOneSchedulingDate(task.schedulings)) || schedulingDates.nonEmpty, invalidSchedulingDateFormat))
        ::: areValidExclusions(exclusions, startDate, endDate)
        ::: areValidSchedulings(schedulings, startDate, endDate)).filter(errorList => errorList match {
          case (isValid, _) => !isValid
        })
    }

    errorList.map(errList =>

      if (errList.isEmpty) Right(TaskDTO(taskId, task.fileName, task.taskType, startDate, task.periodType, task.period, endDate, task.occurrences, task.occurrences, timezoneString, exclusions, schedulings))
      else Left(errList.unzip match {
        case (_, errors) => errors
      }))
  }

  def updateValidator(id: String, task: UpdateTaskDTO): Future[Either[List[Error], TaskDTO]] = {

    taskRepo.selectTask(id).flatMap(maybeDTO =>

      if (maybeDTO.isDefined) {

        val dto = maybeDTO.get

        println("id: " + id)
        println("task: " + task)
        println("oldTask: " + dto)

        val startDate = isValidStartDateFormat(task.startDateAndTime, task.timezone)
        val endDate = isValidEndDateFormat(task.endDateAndTime, task.timezone)
        val timezone = isValidTimezone(task.timezone)
        val timezoneString = if (timezone.isDefined) Some(timezone.get.getID) else None
        val exclusionDates = areValidUpdateExclusionDateFormats(task.exclusions, startDate, endDate)
        val exclusions = areValidUpdateExclusionFormats(task.exclusions, exclusionDates, task.taskId.getOrElse(dto.taskId))
        val schedulingDates = areValidUpdateSchedulingDateFormats(task.schedulings, startDate, endDate)
        val schedulings = areValidUpdateSchedulingFormats(task.schedulings, schedulingDates, task.taskId.getOrElse(dto.taskId))
        val resultDto = isValidUpdateTask(task, dto, startDate, endDate, timezoneString, exclusions, schedulings)

        //println(resultDto)

        val errorList: Future[List[(Boolean, Error)]] = isValidFileName(task.fileName).map { validFileName =>

          (List(
            (resultDto.isDefined, invalidUpdateTaskFormat),
            (isValidListOfDeletes(task), invalidTaskDeleteList),
            (isValidUUID(task.taskId), invalidTaskUUID),
            (task.startDateAndTime.isEmpty || startDate.isDefined, invalidStartDateFormat),
            (isValidStartDateValue(startDate), invalidStartDateValue),
            (validFileName, invalidFileName),
            (isValidTaskType(task.taskType) || task.taskType.isEmpty, invalidTaskType),
            (isValidPeriodType(task.periodType), invalidPeriodType),
            (isValidPeriod(task.period), invalidPeriod),
            (task.endDateAndTime.isEmpty || endDate.isDefined, invalidEndDateFormat),
            (isValidEndDateValue(startDate, endDate), invalidEndDateValue),
            (isValidOccurrences(task.occurrences), invalidOccurrences),
            (task.timezone.isEmpty || timezoneString.isDefined, invalidTimezone),
            (task.exclusions.isEmpty || exclusions.isDefined, invalidExclusionFormat),
            ((exclusionDates.isEmpty && !existsAtLeastOneUpdateExclusionDate(task.exclusions)) || exclusionDates.isDefined, invalidExclusionDateFormat),
            (task.schedulings.isEmpty || schedulings.isDefined, invalidSchedulingFormat),
            ((schedulingDates.isEmpty && !existsAtLeastOneUpdateSchedulingDate(task.schedulings)) || schedulingDates.isDefined, invalidSchedulingDateFormat))
            ::: areValidExclusions(exclusions, startDate, endDate)
            ::: areValidSchedulings(schedulings, startDate, endDate)).filter(errorList => errorList match {
              case (isValid, _) => !isValid
            })
        }

        errorList.map(errList =>
          if (errList.isEmpty) {
            Right(resultDto.get)
          } else Left(errList.unzip match {
            case (_, errors) => errors
          }))
      } else Future.successful(Left(List(invalidEndpointId))))
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
        task.periodType.isDefined && task.period.isDefined && task.schedulings.isDefined &&
          ((task.endDateAndTime.isDefined && task.occurrences.isEmpty) || (task.endDateAndTime.isEmpty && task.occurrences.isDefined))
      case _ => false
    }
  }

  private def isValidUpdateTask(task: UpdateTaskDTO, oldTask: TaskDTO, startDate: Option[Date], endDate: Option[Date], timezoneString: Option[String], exclusions: Option[List[ExclusionDTO]], schedulings: Option[List[SchedulingDTO]]): Option[TaskDTO] = {

    val resultDto = TaskDTO(
      task.taskId.getOrElse(oldTask.taskId), //taskId
      task.fileName.getOrElse(oldTask.fileName), //fileName
      task.taskType.getOrElse(oldTask.taskType), //taskType
      if (task.startDateAndTime.isDefined) startDate else { if (task.toDelete.contains("startDateAndTime")) None else oldTask.startDateAndTime }, //startDate
      if (task.periodType.isDefined) task.periodType else { if (task.toDelete.contains("periodType")) None else oldTask.periodType }, //periodType
      if (task.period.isDefined) task.period else { if (task.toDelete.contains("period")) None else oldTask.period }, //period
      if (task.endDateAndTime.isDefined) endDate else { if (task.toDelete.contains("endDateAndTime")) None else oldTask.endDateAndTime }, //endDate
      if (task.occurrences.isDefined) task.occurrences else { if (task.toDelete.contains("occurrences")) None else oldTask.totalOccurrences }, //totalOccurrences
      if (task.occurrences.isDefined) task.occurrences else { if (task.toDelete.contains("occurrences")) None else oldTask.currentOccurrences }, //currentOccurrences
      if (task.timezone.isDefined) timezoneString else { if (task.toDelete.contains("timezone")) None else oldTask.timezone }, //timezone
      if (task.exclusions.isDefined) exclusions else { if (task.toDelete.contains("exclusions")) None else oldTask.exclusions }, //exclusions
      if (task.schedulings.isDefined) schedulings else { if (task.toDelete.contains("schedulings")) None else oldTask.schedulings } //schedulings
    )

    println("resultDTO: " + resultDto)

    val result = resultDto.taskType match {
      case SchedulingType.RunOnce =>
        (task.toDelete.contains("period") || (task.period.isEmpty && oldTask.period.isEmpty)) &&
          (task.toDelete.contains("endDateAndTime") || (task.endDateAndTime.isEmpty && oldTask.endDateAndTime.isEmpty)) &&
          (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.totalOccurrences.isEmpty)) &&
          (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.currentOccurrences.isEmpty)) &&
          (task.toDelete.contains("exclusions") || (task.exclusions.isEmpty && oldTask.exclusions.isEmpty)) &&
          (task.toDelete.contains("schedulings") || (task.schedulings.isEmpty && oldTask.schedulings.isEmpty))
      case SchedulingType.Periodic =>
        (!task.toDelete.contains("periodType") && (task.periodType.isDefined || oldTask.periodType.isDefined)) &&
          (!task.toDelete.contains("period") && (task.period.isDefined || oldTask.period.isDefined)) &&
          (task.toDelete.contains("schedulings") || (task.schedulings.isEmpty && oldTask.schedulings.isEmpty)) &&
          (((!task.toDelete.contains("endDateAndTime") && (task.endDateAndTime.isDefined || oldTask.endDateAndTime.isDefined)) && (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.totalOccurrences.isEmpty)) && (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.currentOccurrences.isEmpty))) ||
            ((task.toDelete.contains("endDateAndTime") || (task.endDateAndTime.isEmpty && oldTask.endDateAndTime.isEmpty)) && (!task.toDelete.contains("occurrences") && (task.occurrences.isDefined || oldTask.totalOccurrences.isDefined)) && (!task.toDelete.contains("occurrences") && (task.occurrences.isDefined || oldTask.currentOccurrences.isDefined))))
      case SchedulingType.Personalized =>
        (!task.toDelete.contains("periodType") && (task.periodType.isDefined || oldTask.periodType.isDefined)) &&
          (!task.toDelete.contains("period") && (task.period.isDefined || oldTask.period.isDefined)) &&
          (!task.toDelete.contains("schedulings") && (task.schedulings.isDefined || oldTask.schedulings.isDefined)) &&
          (((!task.toDelete.contains("endDateAndTime") && (task.endDateAndTime.isDefined || oldTask.endDateAndTime.isDefined)) && (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.totalOccurrences.isEmpty)) && (task.toDelete.contains("occurrences") || (task.occurrences.isEmpty && oldTask.currentOccurrences.isEmpty))) ||
            ((task.toDelete.contains("endDateAndTime") || (task.endDateAndTime.isEmpty && oldTask.endDateAndTime.isEmpty)) && (!task.toDelete.contains("occurrences") && (task.occurrences.isDefined || oldTask.totalOccurrences.isDefined)) && (!task.toDelete.contains("occurrences") && (task.occurrences.isDefined || oldTask.currentOccurrences.isDefined))))
      case _ => false
    }

    if (result) Some(resultDto) else None
  }

  private def isValidListOfDeletes(task: UpdateTaskDTO): Boolean = {
    if (task.toDelete.nonEmpty) {
      def iter(iterList: List[String]): Boolean = {
        if (iterList.nonEmpty) {
          iterList.head match {
            case "startDateAndTime" => if (task.startDateAndTime.isEmpty) iter(iterList.tail) else false
            case "periodType" => if (task.periodType.isEmpty) iter(iterList.tail) else false
            case "period" => if (task.period.isEmpty) iter(iterList.tail) else false
            case "endDateAndTime" => if (task.endDateAndTime.isEmpty) iter(iterList.tail) else false
            case "occurrences" => if (task.occurrences.isEmpty) iter(iterList.tail) else false
            case "timezone" => if (task.timezone.isEmpty) iter(iterList.tail) else false
            case "exclusions" => if (task.exclusions.isEmpty) iter(iterList.tail) else false
            case "schedulings" => if (task.schedulings.isEmpty) iter(iterList.tail) else false
            case _ => false
          }
        } else true
      }
      iter(task.toDelete)
    } else true

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
   * @param startDate The Date to be checked
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
  private def isValidFileName(fileName: Option[String]): Future[Boolean] = {
    if (fileName.isDefined) fileRepo.existsCorrespondingFileName(fileName.get)
    else Future.successful(true)
  }

  private def isValidTaskType(taskType: Option[String]): Boolean = {
    if (taskType.isDefined) taskType.get.equals("RunOnce") || taskType.get.equals("Periodic") || taskType.get.equals("Personalized")
    else false
  }

  private def isValidPeriodType(periodType: Option[String]): Boolean = {
    periodType.isEmpty || periodTypeList.contains(periodType.getOrElse(""))
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

  //TODO fix exclusionDates.tail
  private def areValidUpdateExclusionFormats(exclusions: Option[List[UpdateExclusionDTO]], exclusionDates: Option[List[Option[Date]]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(exclusions: List[UpdateExclusionDTO], exclusionDates: List[Option[Date]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if (exclusions.isEmpty) { if (toReturn.isEmpty) None else Some(toReturn) }
      else {
        val exclusion = exclusions.head
        exclusion.exclusionDate match {
          case Some(_) =>
            val exclusionDate = if (exclusionDates.nonEmpty) exclusionDates.head else None
            if (exclusion.day.isEmpty && exclusion.dayOfWeek.isEmpty && exclusion.dayType.isEmpty
              && exclusion.month.isEmpty && exclusion.year.isEmpty && exclusion.criteria.isEmpty) {
              iter(exclusions.tail, if (exclusionDates.nonEmpty) exclusionDates.tail else List(), ExclusionDTO(UUIDGen.generateUUID, taskId, exclusionDate) :: toReturn)
            } else None
          case None =>
            if (exclusion.day.isDefined || exclusion.dayOfWeek.isDefined || exclusion.dayType.isDefined
              || exclusion.month.isDefined || exclusion.year.isDefined) {
              iter(exclusions.tail, if (exclusionDates.nonEmpty) exclusionDates.tail else List(), ExclusionDTO(UUIDGen.generateUUID, taskId, None, exclusion.day, exclusion.dayOfWeek, exclusion.dayType, exclusion.month, exclusion.year, exclusion.criteria) :: toReturn)
            } else None
        }
      }
    }
    exclusions match {
      case Some(exclusionList) => iter(exclusionList, exclusionDates.getOrElse(List()), Nil)
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
    if (exclusions.isDefined) iter(exclusions.getOrElse(Nil), Nil)
    else None
  }

  private def getOldExclusionWithExclusionId(exclusionId: Option[String], oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
    def iter(oldExclusions: Option[List[ExclusionDTO]]): Option[ExclusionDTO] = {
      if ((oldExclusions.isDefined && oldExclusions.get.isEmpty) || exclusionId.isEmpty) None
      else if (oldExclusions.get.head.exclusionId.equals(exclusionId.getOrElse(""))) oldExclusions.map(_.head)
      else iter(oldExclusions.map(_.tail))
    }
    iter(oldExclusions)
  }

  private def areValidExclusions(exclusions: Option[List[ExclusionDTO]], startDate: Option[Date], endDate: Option[Date]): List[(Boolean, Error)] = {
    if (exclusions.isDefined) {
      if (startDate.isDefined) startCalendar.setTime(startDate.get)
      else startCalendar.setTime(new Date())
      List(
        (areValidExclusionDateValues(exclusions, endDate), invalidExclusionDateValue),
        (areValidExclusionDayValues(exclusions), invalidExclusionDayValue),
        (areValidExclusionDayOfWeekValues(exclusions), invalidExclusionDayOfWeekValue),
        (areValidExclusionDayTypeValues(exclusions), invalidExclusionDayTypeValue),
        (areValidExclusionMonthValues(exclusions), invalidExclusionMonthValue),
        (areValidExclusionYearValues(exclusions, endDate), invalidExclusionYearValue),
        (areValidExclusionCriteriaValues(exclusions), invalidExclusionCriteriaValue))
    } else Nil

  }

  private def existsAtLeastOneExclusionDate(exclusions: Option[List[CreateExclusionDTO]]): Boolean = {
    exclusions match {
      case Some(list) => list.exists(_.exclusionDate.nonEmpty)
      case None => false
    }
  }

  private def existsAtLeastOneUpdateExclusionDate(exclusions: Option[List[UpdateExclusionDTO]]): Boolean = {
    exclusions match {
      case Some(list) => list.exists(_.exclusionDate.nonEmpty)
      case None => false
    }
  }

  private def areValidExclusionFormats(exclusions: Option[List[CreateExclusionDTO]], exclusionDates: List[Option[Date]], taskId: String): Option[List[ExclusionDTO]] = {
    def iter(list: List[CreateExclusionDTO], dateList: List[Option[Date]], toReturn: List[ExclusionDTO]): Option[List[ExclusionDTO]] = {
      if (list.isEmpty) Some(toReturn)
      else {
        val exclusion = list.head
        exclusion.exclusionDate match {
          case Some(_) =>
            val exclusionDate = dateList.headOption.flatten
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
            else Nil
          case None => iter(list.tail, None :: toReturn)
        }
      } else toReturn
    }
    if (exclusions.isDefined) iter(exclusions.get, Nil)
    else Nil
  }

  private def areValidExclusionDateValues(exclusions: Option[List[ExclusionDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.exclusionDate.isDefined) {
          if (exclusion.exclusionDate.get.after(startCalendar.getTime)) {
            if (endDate.isDefined)
              if (endDate.get.after(exclusion.exclusionDate.get)) iter(list.tail)
              else false
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

  def areValidExclusionDayValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.day.isDefined) {
          if (exclusion.day.get >= 1 && exclusion.day.get <= 31) {
            if (exclusion.month.isDefined) {
              if (exclusion.year.isDefined) if (isPossibleDate(exclusion.day.get, exclusion.month.get, exclusion.year.get)) iter(list.tail) else false
              else if (isPossibleDateWithoutYear(exclusion.day.get, exclusion.month.get)) iter(list.tail) else false
            } else {
              if (exclusion.year.isDefined) if (isPossibleDateWithoutMonth(exclusion.day.get, exclusion.year.get)) iter(list.tail) else false
              else iter(list.tail)
            }
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
              if (exclusion.dayOfWeek.get >= 2 && exclusion.dayOfWeek.get <= 6)
                if (exclusion.dayType.get == DayType.Weekday) iter(list.tail) else false
              else if (exclusion.dayOfWeek.get == 1 || exclusion.dayOfWeek.get == 7)
                if (exclusion.dayType.get == DayType.Weekend) iter(list.tail) else false
              else false
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
                if ((2 to 6).contains(value)) iter(list.tail)
                else false
              case None => iter(list.tail)
            }
          case Some(DayType.Weekend) =>
            exclusion.dayOfWeek match {
              case Some(value) =>
                if (value == 1 || value == 7) iter(list.tail)
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

  private def areValidExclusionMonthValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.month.isDefined) {
          if (exclusion.month.get >= 1 && exclusion.month.get <= 12) {
            if (exclusion.year.isDefined) {
              if (exclusion.year.get == startCalendar.get(Calendar.YEAR))
                if (exclusion.month.get >= startCalendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if (exclusion.year.get >= startCalendar.get(Calendar.YEAR)) iter(list.tail) else false
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

  private def areValidExclusionYearValues(exclusions: Option[List[ExclusionDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.year.isDefined) {
          if (exclusion.year.get >= startCalendar.get(Calendar.YEAR)) {
            if (endDate.isDefined) {
              val endCalendar: Calendar = Calendar.getInstance
              endCalendar.setTime(endDate.get)
              if (exclusion.year.get <= endCalendar.get(Calendar.YEAR)) iter(list.tail) else false
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

  private def areValidExclusionCriteriaValues(exclusions: Option[List[ExclusionDTO]]): Boolean = {
    def iter(list: List[ExclusionDTO]): Boolean = {
      if (list.nonEmpty) {
        val exclusion = list.head
        if (exclusion.criteria.isDefined)
          if (criteriaList.contains(exclusion.criteria.get)) iter(list.tail) else false
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
            val schedulingDate = schedulingDates.getOrElse(List(None)).headOption.flatten
            if (schedulingDate.isDefined && scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty && scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty)
              iter(list.tail, dateList.map(_.tail), SchedulingDTO(UUIDGen.generateUUID, taskId, schedulingDate) :: toReturn)
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

  private def areValidUpdateSchedulingFormats(schedulings: Option[List[UpdateSchedulingDTO]], schedulingDates: Option[List[Option[Date]]], taskId: String): Option[List[SchedulingDTO]] = {
    def iter(schedulings: List[UpdateSchedulingDTO], schedulingDates: Option[List[Option[Date]]], toReturn: List[SchedulingDTO]): Option[List[SchedulingDTO]] = {

      if (schedulings.isEmpty) {
        if (toReturn.isEmpty) None else Some(toReturn)
      } else {
        val scheduling = schedulings.head
        val schedulingDate = schedulingDates.getOrElse(List(None)).headOption.flatten
        scheduling.schedulingDate match {
          case Some(_) =>
            val schedulingDate = schedulingDates.getOrElse(List(None)).headOption.flatten
            if (scheduling.day.isEmpty && scheduling.dayOfWeek.isEmpty &&
              scheduling.dayType.isEmpty && scheduling.month.isEmpty && scheduling.year.isEmpty && scheduling.criteria.isEmpty) {
              iter(schedulings.tail, schedulingDates.map(_.tail), SchedulingDTO(UUIDGen.generateUUID, taskId, schedulingDate) :: toReturn)
            } else None
          case None =>
            if (scheduling.schedulingDate.isDefined || scheduling.day.isDefined || scheduling.dayOfWeek.isDefined || scheduling.dayType.isDefined
              || scheduling.month.isDefined || scheduling.year.isDefined) {
              iter(schedulings.tail, schedulingDates.map(_.tail), SchedulingDTO(UUIDGen.generateUUID, taskId, schedulingDate, scheduling.day, scheduling.dayOfWeek, scheduling.dayType, scheduling.month, scheduling.year, scheduling.criteria) :: toReturn)
            } else None
        }

      }
    }
    schedulings match {
      case Some(schedulingList) => iter(schedulingList, schedulingDates, Nil)
      case None => None
    }
  }

  private def areValidUpdateSchedulingDateFormats(schedulings: Option[List[UpdateSchedulingDTO]], startDate: Option[Date], endDate: Option[Date]): Option[List[Option[Date]]] = {
    def iter(list: List[UpdateSchedulingDTO], toReturn: List[Option[Date]]): Option[List[Option[Date]]] = {
      if (list.isEmpty) if (toReturn.isEmpty) None else Some(toReturn)
      else {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(date) =>
            val parsedDate = parseDate(date)
            if (parsedDate.isDefined) iter(list.tail, parsedDate :: toReturn)
            else None
          case None => iter(list.tail, None :: toReturn)
        }
      }
    }
    if (schedulings.isDefined) iter(schedulings.get, Nil)
    else None
  }

  private def getOldSchedulingWithSchedulingId(schedulingId: Option[String], oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
    def iter(oldSchedulings: Option[List[SchedulingDTO]]): Option[SchedulingDTO] = {
      if ((oldSchedulings.isEmpty && oldSchedulings.get.isEmpty) || schedulingId.isEmpty) None
      else if (oldSchedulings.get.head.schedulingId.equals(schedulingId.getOrElse(""))) Some(oldSchedulings.get.head)
      else iter(Some(oldSchedulings.get.tail))
    }
    iter(oldSchedulings)
  }

  private def areValidSchedulings(schedulings: Option[List[SchedulingDTO]], startDate: Option[Date], endDate: Option[Date]): List[(Boolean, Error)] = {
    if (schedulings.isDefined) {
      if (startDate.isDefined) startCalendar.setTime(startDate.get) else startCalendar.setTime(new Date())
      List(
        (areValidSchedulingDateValues(schedulings, endDate), invalidSchedulingDateValue),
        (areValidSchedulingDayValues(schedulings), invalidSchedulingDayValue),
        (areValidSchedulingDayOfWeekValues(schedulings), invalidSchedulingDayOfWeekValue),
        (areValidSchedulingDayTypeValues(schedulings), invalidSchedulingDayTypeValue),
        (areValidSchedulingMonthValues(schedulings), invalidSchedulingMonthValue),
        (areValidSchedulingYearValues(schedulings, endDate), invalidSchedulingYearValue),
        (areValidSchedulingCriteriaValues(schedulings), invalidSchedulingCriteriaValue))
    } else Nil
  }

  private def existsAtLeastOneSchedulingDate(schedulings: Option[List[CreateSchedulingDTO]]): Boolean = {
    schedulings match {
      case Some(list) => list.exists(_.schedulingDate.nonEmpty)
      case None => false
    }
  }

  private def existsAtLeastOneUpdateSchedulingDate(schedulings: Option[List[UpdateSchedulingDTO]]): Boolean = {
    schedulings match {
      case Some(list) => list.exists(_.schedulingDate.nonEmpty)
      case None => false
    }
  }

  private def areValidSchedulingFormats(schedulings: Option[List[CreateSchedulingDTO]], schedulingDates: List[Option[Date]], taskId: String): Option[List[SchedulingDTO]] = {
    def iter(list: List[CreateSchedulingDTO], dateList: List[Option[Date]], toReturn: List[SchedulingDTO]): Option[List[SchedulingDTO]] = {
      if (list.isEmpty) Some(toReturn)
      else {
        val scheduling = list.head
        scheduling.schedulingDate match {
          case Some(_) =>
            val schedulingDate = schedulingDates.headOption.flatten
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
            else Nil
          case None => iter(list.tail, None :: toReturn)
        }
      } else toReturn
    }
    if (schedulings.isDefined) iter(schedulings.get, Nil)
    else Nil
  }

  private def areValidSchedulingDateValues(schedulings: Option[List[SchedulingDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.schedulingDate.isDefined) {
          if (scheduling.schedulingDate.get.after(startCalendar.getTime)) {
            if (endDate.isDefined)
              if (endDate.get.after(scheduling.schedulingDate.get)) iter(list.tail)
              else false
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
              if (scheduling.year.isDefined) if (isPossibleDate(scheduling.day.get, scheduling.month.get, scheduling.year.get)) iter(list.tail) else false
              else if (isPossibleDateWithoutYear(scheduling.day.get, scheduling.month.get)) iter(list.tail) else false
            } else {
              if (scheduling.year.isDefined) if (isPossibleDateWithoutMonth(scheduling.day.get, scheduling.year.get)) iter(list.tail) else false
              else iter(list.tail)
            }
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
              if (scheduling.dayOfWeek.get >= 2 && scheduling.dayOfWeek.get <= 6)
                if (scheduling.dayType.get == DayType.Weekday) iter(list.tail) else false
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
              if (scheduling.year.get == startCalendar.get(Calendar.YEAR)) if (scheduling.month.get >= startCalendar.get(Calendar.MONTH)) iter(list.tail) else false
              else if (scheduling.year.get >= startCalendar.get(Calendar.YEAR)) iter(list.tail) else false
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

  private def areValidSchedulingYearValues(schedulings: Option[List[SchedulingDTO]], endDate: Option[Date]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.year.isDefined) {
          if (scheduling.year.get >= startCalendar.get(Calendar.YEAR)) {
            if (endDate.isDefined) {
              val endCalendar: Calendar = Calendar.getInstance
              endCalendar.setTime(endDate.get)
              if (scheduling.year.get <= endCalendar.get(Calendar.YEAR)) iter(list.tail) else false
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

  private def areValidSchedulingCriteriaValues(schedulings: Option[List[SchedulingDTO]]): Boolean = {
    def iter(list: List[SchedulingDTO]): Boolean = {
      if (list.nonEmpty) {
        val scheduling = list.head
        if (scheduling.criteria.isDefined)
          if (criteriaList.contains(scheduling.criteria.get)) iter(list.tail) else false
        else iter(list.tail)
      } else true
    }
    schedulings match {
      case Some(list) => iter(list)
      case None => true
    }
  }

}
