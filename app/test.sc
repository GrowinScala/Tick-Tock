import api.dtos.ExclusionDTO
import api.utils.{FakeUUIDGenerator, UUIDGenerator}
import api.validators.TaskValidator
import database.repositories.file.{FakeFileRepository, FileRepository}
import database.repositories.task.{FakeTaskRepository, TaskRepository}

private implicit val fileRepo: FileRepository = new FakeFileRepository
private implicit val taskRepo: TaskRepository = new FakeTaskRepository
private implicit val uuidGen: UUIDGenerator = new FakeUUIDGenerator

val validator = new TaskValidator
val exclusions = Some(List(ExclusionDTO("asd", "asd", None, Some(31), None, None, Some(1))))

val result = validator.areValidExclusionDayValues(exclusions)
println(result)



