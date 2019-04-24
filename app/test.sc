import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.validators.TaskValidator
import database.repositories.file.{FakeFileRepository, FileRepository}
import database.repositories.task.{FakeTaskRepository, TaskRepository}

implicit val fileRepo: FileRepository = new FakeFileRepository
implicit val taskRepo: TaskRepository = new FakeTaskRepository
implicit val uuidGen: UUIDGenerator = new FakeUUIDGenerator

val validator = new TaskValidator()(fileRepo, taskRepo, uuidGen)
val exclusions = Some(List(ExclusionDTO("asd", "asd", None, Some(31), None, None, Some(2))))

val result = validator.areValidExclusionDayValues(exclusions)
println(result)



