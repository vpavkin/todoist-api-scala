package ru.pavkin.todoist.api.core

import java.util.{UUID, Date}

import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}
import ru.pavkin.todoist.api.core.ToDTO.syntax._
import ru.pavkin.todoist.api.core.model._
import ru.pavkin.todoist.api.core.tags._
import shapeless.tag.@@
import tags.syntax._
import ReminderPeriod._

class ToDTOSpec extends FunSuite with Matchers with GeneratorDrivenPropertyChecks {

  implicit val arbitraryUUID = Arbitrary(Gen.uuid)

  val addProjectGen: Gen[AddProject] = for {
    name <- arbitrary[String]
    color <- Gen.option(Gen.choose(0, 21).map(ProjectColor.unsafeBy))
    indent <- Gen.option(Gen.choose(1, 4).map(Indent.unsafeBy))
    order <- Gen.option(arbitrary[Int])
  } yield AddProject(name, color, indent, order)

  test("AddProject") {
    forAll(addProjectGen) { (p: AddProject) =>
      p.toDTO shouldBe dto.AddProject(
        p.name,
        p.color.map(_.code),
        p.indent.map(_.code),
        p.order
      )
    }
  }

  val addLabelGen: Gen[AddLabel] = for {
    name <- arbitrary[String]
    color <- Gen.option(Gen.choose(0, 12).map(LabelColor.unsafeBy))
    order <- Gen.option(arbitrary[Int])
  } yield AddLabel(name, color, order)

  test("AddLabel") {
    forAll(addLabelGen) { (p: AddLabel) =>
      p.toDTO shouldBe dto.AddLabel(
        p.name,
        p.color.map(_.code),
        p.order
      )
    }
  }

  val taskDateGen: Gen[TaskDate] = for {
    str <- arbitrary[Option[String]]
    lang <- Gen.oneOf(DateLanguage.en,
      DateLanguage.da,
      DateLanguage.pl,
      DateLanguage.zh,
      DateLanguage.ko,
      DateLanguage.de,
      DateLanguage.pt,
      DateLanguage.ja,
      DateLanguage.it,
      DateLanguage.fr,
      DateLanguage.sv,
      DateLanguage.ru,
      DateLanguage.es,
      DateLanguage.nl
    )
    date <- arbitrary[Date]
  } yield TaskDate(str, lang, date)

  def addTaskGen[T: IsResourceId](gen: Gen[T]): Gen[AddTask[T]] = for {
    content <- arbitrary[String]
    projectId <- gen.map(_.projectId)
    date <- Gen.option(taskDateGen)
    priority <- Gen.option(Gen.oneOf(Priority.level1, Priority.level2, Priority.level3, Priority.level4))
    indent <- Gen.option(Gen.oneOf(Indent.level1, Indent.level2, Indent.level3, Indent.level4))
    order <- Gen.option(arbitrary[Int])
    dayOrder <- Gen.option(arbitrary[Int])
    isCollapsed <- arbitrary[Option[Boolean]]
    labels <- arbitrary[List[Int]].map(_.map(_.labelId))
    assignedBy <- arbitrary[Option[Int]].map(_.map(_.userId))
    responsible <- arbitrary[Option[Int]].map(_.map(_.userId))
  } yield AddTask(
    content, projectId, date, priority, indent, order, dayOrder, isCollapsed, labels, assignedBy, responsible
  )

  test("AddTask") {
    forAll(addTaskGen(Gen.uuid)) { (p: AddTask[UUID]) =>
      p.toDTO shouldBe dto.AddTask(
        p.content,
        p.projectId: UUID,
        p.date.flatMap(_.text),
        p.date.map(_.language.code),
        p.date.map(_.dueDateUTC).map(TodoistDate.format),
        p.priority.map(_.level),
        p.indent.map(_.code),
        p.order,
        p.dayOrder,
        p.isCollapsed.map(b => if (b) 1 else 0),
        p.labels,
        p.assignedBy,
        p.responsible
      )
    }
  }

  val addTaskToInboxGen: Gen[AddTaskToInbox] = addTaskGen(Gen.uuid).map(p => AddTaskToInbox(
    p.content,
    p.date,
    p.priority,
    p.indent,
    p.order,
    p.dayOrder,
    p.isCollapsed,
    p.labels
  ))

  test("AddTaskToInbox") {
    forAll(addTaskToInboxGen) { (p: AddTaskToInbox) =>
      p.toDTO shouldBe dto.AddTaskToInbox(
        p.content,
        p.date.flatMap(_.text),
        p.date.map(_.language.code),
        p.date.map(_.dueDateUTC).map(TodoistDate.format),
        p.priority.map(_.level),
        p.indent.map(_.code),
        p.order,
        p.dayOrder,
        p.isCollapsed.map(b => if (b) 1 else 0),
        p.labels
      )
    }
  }

  val addFilterGen: Gen[AddFilter] = for {
    name <- arbitrary[String]
    query <- arbitrary[String]
    color <- Gen.choose(0, 12).map(LabelColor.unsafeBy)
    order <- arbitrary[Option[Int]]
  } yield AddFilter(name, query, color, order)

  test("AddFilter") {
    forAll(addFilterGen) { (p: AddFilter) =>
      p.toDTO shouldBe dto.AddFilter(
        p.name, p.query, p.color.code, p.order
      )
    }
  }

  def addRelativeReminderGen[T: IsResourceId](gen: Gen[T]): Gen[AddRelativeTimeBasedReminder[T]] = for {
    task <- gen.map(_.taskId)
    service <- Gen.oneOf(ReminderService.Push, ReminderService.Email, ReminderService.SMS)
    minutes <- Gen.oneOf(min30, min45, hour1, hour2, hour3, day1, day2, day3, week)
    subscriber <- arbitrary[Option[Int]].map(_.map(_.userId))
  } yield AddRelativeTimeBasedReminder(
    task, service, minutes, subscriber
  )

  def addAbsoluteReminderGen[T: IsResourceId](gen: Gen[T]): Gen[AddAbsoluteTimeBasedReminder[T]] = for {
    task <- gen.map(_.taskId)
    service <- Gen.oneOf(ReminderService.Push, ReminderService.Email, ReminderService.SMS)
    date <- taskDateGen
    subscriber <- arbitrary[Option[Int]].map(_.map(_.userId))
  } yield AddAbsoluteTimeBasedReminder(
    task, service, date, subscriber
  )

  def addLocationReminderGen[T: IsResourceId](gen: Gen[T]): Gen[AddLocationBasedReminder[T]] = for {
    task <- gen.map(_.taskId)
    subscriber <- arbitrary[Option[Int]].map(_.map(_.userId))
    name <- arbitrary[String]
    lat <- arbitrary[Double]
    lon <- arbitrary[Double]
    trigger <- Gen.oneOf(LocationBasedReminder.TriggerKind.Enter, LocationBasedReminder.TriggerKind.Leave)
    radius <- Gen.posNum[Int]
  } yield AddLocationBasedReminder(
    task, name, lat, lon, trigger, radius, subscriber
  )

  test("AddRelativeTimeBasedReminder") {
    forAll(addRelativeReminderGen(Gen.uuid)) { (p: AddRelativeTimeBasedReminder[UUID]) =>
      p.toDTO shouldBe dto.AddReminder(
        p.taskId: UUID,
        "relative",
        p.subscriber,
        Some(p.service.name),
        minute_offset = Some(p.minutesBefore.minutes)
      )
    }
  }

  test("AddAbsoluteTimeBasedReminder") {
    forAll(addAbsoluteReminderGen(arbitrary[Int])) { (p: AddAbsoluteTimeBasedReminder[Int]) =>
      p.toDTO shouldBe dto.AddReminder(
        p.taskId: Int,
        "absolute",
        p.subscriber,
        Some(p.service.name),
        date_string = p.dueDate.text,
        date_lang = Some(p.dueDate.language.code),
        due_date_utc = Some(TodoistDate.format(p.dueDate.dueDateUTC))
      )
    }
  }

  test("AddLocationBasedReminder") {
    forAll(addLocationReminderGen(arbitrary[Int])) { (p: AddLocationBasedReminder[Int]) =>
      p.toDTO shouldBe dto.AddReminder(
        p.taskId: Int,
        "location",
        p.subscriber,
        name = Some(p.locationName),
        loc_lat = Some(p.latitude.toString),
        loc_long = Some(p.longitude.toString),
        loc_trigger = Some(p.triggerKind.name),
        radius = Some(p.radiusInMeters)
      )
    }
  }

  def addNoteGen[T: IsResourceId](gen: Gen[T]): Gen[AddNote[T]] = for {
    content <- arbitrary[String]
    taskId <- gen.map(_.taskId)
    notify <- Gen.listOf(Gen.posNum[Int]).map(_.userIds)
  } yield AddNote(
    content, taskId, notify
  )

  test("AddNote") {
    forAll(addNoteGen(Gen.uuid)) { (p: AddNote[UUID]) =>
      p.toDTO shouldBe dto.AddNote(
        p.content,
        p.taskId: UUID,
        p.notifyUsers.map(a => a: Int)
      )
    }
  }

  def updateProjectGen[T: IsResourceId](gen: Gen[T]): Gen[UpdateProject[T]] = for {
    o <- arbitrary[Option[Int]]
    p <- addProjectGen
    collapsed <- arbitrary[Option[Boolean]]
    id <- gen
  } yield UpdateProject[T](
    id.projectId, o.map(_ => p.name), p.color, p.indent, p.order, collapsed
  )

  test("UpdateProject") {
    forAll(updateProjectGen(Gen.uuid)) { (p: UpdateProject[UUID]) =>
      p.toDTO shouldBe dto.UpdateProject(
        p.id: UUID,
        p.name,
        p.color.map(_.code),
        p.indent.map(_.code),
        p.order,
        p.isCollapsed.map(b => if (b) 1 else 0)
      )
    }
  }

  def updateLabelGen[T: IsResourceId](gen: Gen[T]): Gen[UpdateLabel[T]] = for {
    o <- arbitrary[Option[Int]]
    p <- addLabelGen
    id <- gen
  } yield UpdateLabel[T](
    id.labelId, o.map(_ => p.name), p.color, p.order
  )

  test("UpdateLabel") {
    forAll(updateLabelGen(arbitrary[Int])) { (p: UpdateLabel[Int]) =>
      p.toDTO shouldBe dto.UpdateLabel(
        p.id: Int,
        p.name,
        p.color.map(_.code),
        p.order
      )
    }
  }

  def updateNoteGen[T: IsResourceId](gen: Gen[T]): Gen[UpdateNote[T]] = for {
    id <- gen
    content <- arbitrary[Option[String]]
  } yield UpdateNote[T](
    id.noteId, content
  )

  test("UpdateNote") {
    forAll(updateNoteGen(arbitrary[Int])) { (p: UpdateNote[Int]) =>
      p.toDTO shouldBe dto.UpdateNote(
        p.id: Int, p.content
      )
    }
  }

  def updateFilterGen[T: IsResourceId](gen: Gen[T]): Gen[UpdateFilter[T]] = for {
    id <- gen
    name <- arbitrary[Option[String]]
    query <- arbitrary[Option[String]]
    color <- Gen.option(Gen.choose(0, 12).map(LabelColor.unsafeBy))
    order <- arbitrary[Option[Int]]
  } yield UpdateFilter[T](
    id.filterId, name, query, color, order
  )

  test("UpdateFilter") {
    forAll(updateFilterGen(arbitrary[Int])) { (p: UpdateFilter[Int]) =>
      p.toDTO shouldBe dto.UpdateFilter(
        p.id: Int,
        p.name,
        p.query,
        p.color.map(_.code),
        p.order
      )
    }
  }
  def updateTaskGen[T: IsResourceId](gen: Gen[T]): Gen[UpdateTask[T]] = for {
    o <- arbitrary[Option[Int]]
    p <- addTaskGen(Gen.uuid)
    id <- gen
  } yield UpdateTask[T](
    id.taskId, o.map(_ => p.content), p.date, p.priority, p.indent,
    p.order, p.dayOrder, p.isCollapsed, p.labels, p.assignedBy, p.responsible
  )

  test("UpdateTask") {
    forAll(updateTaskGen(arbitrary[Int])) { (p: UpdateTask[Int]) =>
      p.toDTO shouldBe dto.UpdateTask(
        p.id: Int,
        p.content,
        p.date.flatMap(_.text),
        p.date.map(_.language.code),
        p.date.map(_.dueDateUTC).map(TodoistDate.format),
        p.priority.map(_.level),
        p.indent.map(_.code),
        p.order,
        p.dayOrder,
        p.isCollapsed.map(b => if (b) 1 else 0),
        p.labels,
        p.assignedBy,
        p.responsible
      )
    }
  }

  val moveTasksGen: Gen[MoveTasks] = for {
    tasks <- arbitrary[Map[Int, List[Int]]].map(_.map { case (k, v) => k.projectId -> v.taskIds })
    p <- arbitrary[Int]
  } yield MoveTasks(tasks, p.projectId)

  test("MoveTasks") {
    forAll(moveTasksGen) { (p: MoveTasks) =>
      p.toDTO shouldBe dto.MoveTasks(
        p.tasks.map { case (k, v) => k.toString -> v.map(a => a: Int) },
        p.toProject: Int
      )
    }
  }

  def singleIdCommandTest[T: IsResourceId, C, Tag](name: String,
                                                   commandFactory: T => C,
                                                   extractor: C => T @@ Tag)
                                                  (implicit gen: Arbitrary[T],
                                                   toDTO: ToDTO[C, dto.SingleIdCommand[T]]) = {
    val generator = arbitrary[T].map(commandFactory)

    test(name) {
      forAll(generator) { (p: C) =>
        p.toDTO shouldBe dto.SingleIdCommand[T](extractor(p): T)
      }
    }
  }

  singleIdCommandTest[Int, CloseTask[Int], TaskId](
    "CloseTask",
    id => CloseTask(id.taskId),
    _.task
  )

  singleIdCommandTest[Int, DeleteFilter[Int], FilterId](
    "DeleteFilter",
    id => DeleteFilter(id.filterId),
    _.filter
  )

  singleIdCommandTest[Int, DeleteNote[Int], NoteId](
    "DeleteNote",
    id => DeleteNote(id.noteId),
    _.note
  )

  singleIdCommandTest[Int, DeleteReminder[Int], ReminderId](
    "DeleteReminder",
    id => DeleteReminder(id.reminderId),
    _.reminder
  )

  singleIdCommandTest[Int, DeleteLabel[Int], LabelId](
    "DeleteLabel",
    id => DeleteLabel(id.labelId),
    _.label
  )
  def multipleIdCommandTest[T: IsResourceId, C, Tag](name: String,
                                                     commandFactory: List[T] => C,
                                                     extractor: C => List[T @@ Tag])
                                                    (implicit gen: Arbitrary[T],
                                                     toDTO: ToDTO[C, dto.MultipleIdCommand[T]]) = {
    val generator = for {
      ids <- Gen.nonEmptyListOf(arbitrary[T])
    } yield commandFactory(ids)
    test(name) {
      forAll(generator) { (p: C) =>
        p.toDTO shouldBe dto.MultipleIdCommand[T](extractor(p).map(a => a: T))
      }
    }
  }

  multipleIdCommandTest[Int, DeleteProjects[Int], ProjectId](
    "DeleteProjects",
    ids => DeleteProjects(ids.projectIds),
    _.projects
  )

  multipleIdCommandTest[UUID, ArchiveProjects[UUID], ProjectId](
    "ArchiveProjects",
    ids => ArchiveProjects(ids.projectIds),
    _.projects
  )

  multipleIdCommandTest[Int, UnarchiveProjects[Int], ProjectId](
    "UnarchiveProjects",
    ids => UnarchiveProjects(ids.projectIds),
    _.projects
  )

  multipleIdCommandTest[Int, UncompleteTasks[Int], TaskId](
    "UncompleteTasks",
    ids => UncompleteTasks(ids.taskIds),
    _.tasks
  )

  multipleIdCommandTest[Int, DeleteTasks[Int], TaskId](
    "DeleteTasks",
    ids => DeleteTasks(ids.taskIds),
    _.tasks
  )
}

