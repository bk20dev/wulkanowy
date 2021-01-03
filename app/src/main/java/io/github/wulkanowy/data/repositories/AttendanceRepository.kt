package io.github.wulkanowy.data.repositories

import io.github.wulkanowy.data.db.dao.AttendanceDao
import io.github.wulkanowy.data.db.entities.Attendance
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.mappers.mapToEntities
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Absent
import io.github.wulkanowy.utils.init
import io.github.wulkanowy.utils.monday
import io.github.wulkanowy.utils.networkBoundResource
import io.github.wulkanowy.utils.sunday
import io.github.wulkanowy.utils.uniqueSubtract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDb: AttendanceDao,
    private val sdk: Sdk
) {

    fun getAttendance(student: Student, semester: Semester, start: LocalDate, end: LocalDate, forceRefresh: Boolean) = networkBoundResource(
        shouldFetch = { it.isEmpty() || forceRefresh },
        query = { attendanceDb.loadAll(semester.diaryId, semester.studentId, start.monday, end.sunday) },
        fetch = {
            sdk.init(student).switchDiary(semester.diaryId, semester.schoolYear)
                .getAttendance(start.monday, end.sunday, semester.semesterId)
                .mapToEntities(semester)
        },
        saveFetchResult = { old, new ->
            attendanceDb.deleteAll(old uniqueSubtract new)
            attendanceDb.insertAll(new uniqueSubtract old)
        },
        filterResult = { it.filter { item -> item.date in start..end } }
    )

    suspend fun excuseForAbsence(student: Student, semester: Semester, absenceList: List<Attendance>, reason: String? = null) {
        sdk.init(student).switchDiary(semester.diaryId, semester.schoolYear).excuseForAbsence(absenceList.map { attendance ->
            Absent(
                date = LocalDateTime.of(attendance.date, LocalTime.of(0, 0)),
                timeId = attendance.timeId
            )
        }, reason)
    }
}
