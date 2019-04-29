import java.util.Calendar

import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.utils.DateUtils._
import api.validators.TaskValidator
import database.repositories.file.{FakeFileRepository, FileRepository}
import database.repositories.task.{FakeTaskRepository, TaskRepository}

import scala.util.Try

implicit val fileRepo: FileRepository = new FakeFileRepository
implicit val taskRepo: TaskRepository = new FakeTaskRepository
implicit val uuidGen: UUIDGenerator = new FakeUUIDGenerator

/*val validator = new TaskValidator()(fileRepo, taskRepo, uuidGen)
val exclusions = Some(List(ExclusionDTO("asd", "asd", None, Some(30), None, None, Some(2))))

val result = validator.areValidExclusionDayValues(exclusions)
println(result)*/

val result = isPossibleDate(30, 3, 2039)
println(result)


