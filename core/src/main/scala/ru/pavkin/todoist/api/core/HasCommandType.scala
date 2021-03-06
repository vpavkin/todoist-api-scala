package ru.pavkin.todoist.api.core

import ru.pavkin.todoist.api.core.model._

trait HasCommandType[T] {
  def commandType: String
}

object HasCommandType {

  object syntax {
    implicit class CommandTypeOps[T](o: T)(implicit ev: HasCommandType[T]) {
      def commandType = ev.commandType
    }
  }

  def apply[T](s: String): HasCommandType[T] = new HasCommandType[T] {
    def commandType: String = s
  }

  implicit val addProject: HasCommandType[AddProject] = HasCommandType("project_add")
  implicit def addTask[T: IsResourceId]: HasCommandType[AddTask[T]] = HasCommandType("item_add")
  implicit val addTaskToInbox: HasCommandType[AddTaskToInbox] = HasCommandType("item_add")
  implicit def addNote[T: IsResourceId]: HasCommandType[AddNote[T]] = HasCommandType("note_add")
  implicit def addRelativeReminder[T: IsResourceId]: HasCommandType[AddRelativeTimeBasedReminder[T]] =
    HasCommandType("reminder_add")
  implicit def addAbsoluteReminder[T: IsResourceId]: HasCommandType[AddAbsoluteTimeBasedReminder[T]] =
    HasCommandType("reminder_add")
  implicit def addLocationReminder[T: IsResourceId]: HasCommandType[AddLocationBasedReminder[T]] =
    HasCommandType("reminder_add")
  implicit val addLabel: HasCommandType[AddLabel] = HasCommandType("label_add")
  implicit val addFilter: HasCommandType[AddFilter] = HasCommandType("filter_add")

  implicit def updateProject[T: IsResourceId]: HasCommandType[UpdateProject[T]] = HasCommandType("project_update")
  implicit def updateTask[T: IsResourceId]: HasCommandType[UpdateTask[T]] = HasCommandType("item_update")
  implicit def updateLabel[T: IsResourceId]: HasCommandType[UpdateLabel[T]] = HasCommandType("label_update")
  implicit def updateFilter[T: IsResourceId]: HasCommandType[UpdateFilter[T]] = HasCommandType("filter_update")
  implicit def updateNote[T: IsResourceId]: HasCommandType[UpdateNote[T]] = HasCommandType("note_update")

  implicit def deleteProjects[T: IsResourceId]: HasCommandType[DeleteProjects[T]] = HasCommandType("project_delete")
  implicit def deleteTasks[T: IsResourceId]: HasCommandType[DeleteTasks[T]] = HasCommandType("item_delete")
  implicit def deleteLabel[T: IsResourceId]: HasCommandType[DeleteLabel[T]] = HasCommandType("label_delete")
  implicit def deleteNote[T: IsResourceId]: HasCommandType[DeleteNote[T]] = HasCommandType("note_delete")
  implicit def deleteFilter[T: IsResourceId]: HasCommandType[DeleteFilter[T]] = HasCommandType("filter_delete")
  implicit def deleteReminder[T: IsResourceId]: HasCommandType[DeleteReminder[T]] = HasCommandType("reminder_delete")

  implicit def closeTask[T: IsResourceId]: HasCommandType[CloseTask[T]] = HasCommandType("item_close")
  implicit def uncompleteTasks[T: IsResourceId]: HasCommandType[UncompleteTasks[T]] = HasCommandType("item_uncomplete")
  implicit val moveTasks: HasCommandType[MoveTasks] = HasCommandType("item_move")

  implicit def archiveProjects[T: IsResourceId]: HasCommandType[ArchiveProjects[T]] = HasCommandType("project_archive")
  implicit def unarchiveProjects[T: IsResourceId]: HasCommandType[UnarchiveProjects[T]] =
    HasCommandType("project_unarchive")
}
