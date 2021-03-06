package ru.pavkin.todoist.api.core

import cats.Functor
import cats.syntax.functor._
import ru.pavkin.todoist.api.core.model._
import ru.pavkin.todoist.api.utils.Produce

trait ToDTO[Model, DTO] extends Produce[Model, DTO]

object ToDTO {

  def apply[Model, DTO](f: Model => DTO): ToDTO[Model, DTO] = new ToDTO[Model, DTO] {
    def produce(a: Model): DTO = f(a)
  }

  object syntax {
    implicit class ToDTOSyntaxOps[Model, DTO](a: Model)(implicit F: ToDTO[Model, DTO]) {
      def toDTO: DTO = F.produce(a)
    }
  }

  private implicit class ToIntBoolOps(b: Boolean) {
    def toInt = if (b) 1 else 0
  }

  implicit def functorToDTO[F[_] : Functor, Model, DTO](implicit F: ToDTO[Model, DTO]): ToDTO[F[Model], F[DTO]] =
    new ToDTO[F[Model], F[DTO]] {
      def produce(a: F[Model]): F[DTO] = a.map(F.produce)
    }

  implicit val addProjectToDTO: ToDTO[AddProject, dto.AddProject] = ToDTO(a => dto.AddProject(
    a.name, a.color.map(_.code), a.indent.map(_.code), a.order
  ))

  implicit def addTaskToDTO[T: IsResourceId]: ToDTO[AddTask[T], dto.AddTask[T]] =
    ToDTO(a => dto.AddTask[T](
      a.content,
      a.projectId,
      a.date.flatMap(_.text),
      a.date.map(_.language.code),
      a.date.map(_.dueDateUTC).map(TodoistDate.format),
      a.priority.map(_.level),
      a.indent.map(_.code),
      a.order,
      a.dayOrder,
      a.isCollapsed.map(_.toInt),
      a.labels,
      a.assignedBy,
      a.responsible
    ))

  implicit val addTaskToInboxToDTO: ToDTO[AddTaskToInbox, dto.AddTaskToInbox] =
    ToDTO(a => dto.AddTaskToInbox(
      a.content,
      a.date.flatMap(_.text),
      a.date.map(_.language.code),
      a.date.map(_.dueDateUTC).map(TodoistDate.format),
      a.priority.map(_.level),
      a.indent.map(_.code),
      a.order,
      a.dayOrder,
      a.isCollapsed.map(_.toInt),
      a.labels
    ))

  implicit val addLabelToDTO: ToDTO[AddLabel, dto.AddLabel] =
    ToDTO(a => dto.AddLabel(
      a.name,
      a.color.map(_.code),
      a.order
    ))

  implicit val addFilterToDTO: ToDTO[AddFilter, dto.AddFilter] =
    ToDTO(a => dto.AddFilter(
      a.name,
      a.query,
      a.color.code,
      a.order
    ))

  implicit def addNoteToDTO[T: IsResourceId]: ToDTO[AddNote[T], dto.AddNote[T]] =
    ToDTO(a => dto.AddNote[T](
      a.content,
      a.taskId,
      a.notifyUsers.map(a => a: Int)
    ))

  implicit def addRelativeReminderToDTO[T: IsResourceId]: ToDTO[AddRelativeTimeBasedReminder[T], dto.AddReminder[T]] =
    ToDTO(a => dto.AddReminder[T](
      a.taskId,
      "relative",
      a.subscriber.map(s => s: Int),
      Some(a.service.name),
      minute_offset = Some(a.minutesBefore.minutes)
    ))

  implicit def addAbsoluteReminderToDTO[T: IsResourceId]: ToDTO[AddAbsoluteTimeBasedReminder[T], dto.AddReminder[T]] =
    ToDTO(a => dto.AddReminder[T](
      a.taskId,
      "absolute",
      a.subscriber.map(s => s: Int),
      Some(a.service.name),
      a.dueDate.text,
      Some(a.dueDate.language.code),
      Some(TodoistDate.format(a.dueDate.dueDateUTC))
    ))

  implicit def addLocationReminderToDTO[T: IsResourceId]: ToDTO[AddLocationBasedReminder[T], dto.AddReminder[T]] =
    ToDTO(a => dto.AddReminder[T](
      a.taskId,
      "location",
      a.subscriber.map(s => s: Int),
      name = Some(a.locationName),
      loc_lat = Some(a.latitude.toString),
      loc_long = Some(a.longitude.toString),
      loc_trigger = Some(a.triggerKind.name),
      radius = Some(a.radiusInMeters)
    ))

  implicit def updateTaskToDTO[T: IsResourceId]: ToDTO[UpdateTask[T], dto.UpdateTask[T]] =
    ToDTO(a => dto.UpdateTask[T](
      a.id,
      a.content,
      a.date.flatMap(_.text),
      a.date.map(_.language.code),
      a.date.map(_.dueDateUTC).map(TodoistDate.format),
      a.priority.map(_.level),
      a.indent.map(_.code),
      a.order,
      a.dayOrder,
      a.isCollapsed.map(_.toInt),
      a.labels,
      a.assignedBy,
      a.responsible
    ))

  implicit def updateProjectToDTO[T: IsResourceId]: ToDTO[UpdateProject[T], dto.UpdateProject[T]] =
    ToDTO(a => dto.UpdateProject[T](
      a.id, a.name, a.color.map(_.code), a.indent.map(_.code), a.order, a.isCollapsed.map(_.toInt)
    ))

  implicit def updateLabelToDTO[T: IsResourceId]: ToDTO[UpdateLabel[T], dto.UpdateLabel[T]] =
    ToDTO(a => dto.UpdateLabel[T](
      a.id,
      a.name,
      a.color.map(_.code),
      a.order
    ))

  implicit def updateFilterToDTO[T: IsResourceId]: ToDTO[UpdateFilter[T], dto.UpdateFilter[T]] =
    ToDTO(a => dto.UpdateFilter[T](
      a.id,
      a.name,
      a.query,
      a.color.map(_.code),
      a.order
    ))

  implicit def updateNoteToDTO[T: IsResourceId]: ToDTO[UpdateNote[T], dto.UpdateNote[T]] =
    ToDTO(a => dto.UpdateNote[T](
      a.id,
      a.content
    ))

  implicit def deleteProjectsToDTO[T: IsResourceId]: ToDTO[DeleteProjects[T], dto.MultipleIdCommand[T]] =
    ToDTO(a => dto.MultipleIdCommand[T](a.projects))

  implicit def deleteTasksToDTO[T: IsResourceId]: ToDTO[DeleteTasks[T], dto.MultipleIdCommand[T]] =
    ToDTO(a => dto.MultipleIdCommand[T](a.tasks))

  implicit def deleteLabelToDTO[T: IsResourceId]: ToDTO[DeleteLabel[T], dto.SingleIdCommand[T]] =
    ToDTO(a => dto.SingleIdCommand[T](a.label))

  implicit def deleteFilterToDTO[T: IsResourceId]: ToDTO[DeleteFilter[T], dto.SingleIdCommand[T]] =
    ToDTO(a => dto.SingleIdCommand[T](a.filter))

  implicit def deleteNoteToDTO[T: IsResourceId]: ToDTO[DeleteNote[T], dto.SingleIdCommand[T]] =
    ToDTO(a => dto.SingleIdCommand[T](a.note))

  implicit def deleteReminderToDTO[T: IsResourceId]: ToDTO[DeleteReminder[T], dto.SingleIdCommand[T]] =
    ToDTO(a => dto.SingleIdCommand[T](a.reminder))

  implicit def moveTasksToDTO: ToDTO[MoveTasks, dto.MoveTasks] =
    ToDTO(a => dto.MoveTasks(a.tasks.map {
      case (pId, tasks) => pId.toString -> tasks.map(a => a: Int)
    }, a.toProject: Int))

  implicit def closeTaskToDTO[T: IsResourceId]: ToDTO[CloseTask[T], dto.SingleIdCommand[T]] =
    ToDTO(a => dto.SingleIdCommand[T](a.task))

  implicit def uncompleteTasksToDTO[T: IsResourceId]: ToDTO[UncompleteTasks[T], dto.MultipleIdCommand[T]] =
    ToDTO(a => dto.MultipleIdCommand[T](a.tasks))

  implicit def archiveProjectsToDTO[T: IsResourceId]: ToDTO[ArchiveProjects[T], dto.MultipleIdCommand[T]] =
    ToDTO(a => dto.MultipleIdCommand[T](a.projects))

  implicit def unarchiveProjectsToDTO[T: IsResourceId]: ToDTO[UnarchiveProjects[T], dto.MultipleIdCommand[T]] =
    ToDTO(a => dto.MultipleIdCommand[T](a.projects))
}
