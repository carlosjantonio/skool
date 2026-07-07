# Domain Event Catalog

Every event listed here implements `ao.skool.common.domain.event.DomainEvent`. Names are stable — renaming an event is a breaking change and needs an ADR. Phase 0 has no events yet; this file grows as modules land.

## Naming

- Past tense (`GradePosted`, not `PostGrade`).
- Package: `ao.skool.<producing-module>.api.event.<EventName>`.
- Topic (when Kafka lands): `skool.<producing-module>.<event-snake-case>`.

## Roster

| Producer | Event | Payload sketch | Consumers (planned) |
|----------|-------|----------------|---------------------|
| identity | `UserRegistered` | userId, role, email | notifications |
| identity | `PasswordResetRequested` | userId, resetToken (hashed) | notifications |
| sis | `StudentEnrolled` | studentId, classId, academicYear | reporting, notifications |
| attendance | `AttendanceMarkedAbsent` | studentId, classId, date | notifications |
| grading | `GradePosted` | studentId, subjectId, trimester, value | notifications, reporting |
| grading | `TrimesterClosed` | classId, trimester | reporting |
| assessment | `QuizPublished` | quizId, classId, dueAt | notifications |
| assessment | `QuizSubmitted` | quizId, studentId, submittedAt | notifications |
| assignments | `AssignmentDue` | assignmentId, dueAt, classId | notifications |
| forum | `ForumReplyPosted` | threadId, replyId, parentAuthorId | notifications |
| subject-board | `AnnouncementPosted` | boardId, subjectId, classId | notifications |
| fees | `InvoiceIssued` | invoiceId, studentId, amount, dueDate | notifications |
| fees | `PaymentReceived` | invoiceId, amount, method | notifications, reporting |
| fees | `InvoiceOverdue` | invoiceId, daysOverdue | notifications |

The notifications module decides *how* to deliver — SMS, email, push, WhatsApp, in-app — based on the guardian/user preference stored in identity.
