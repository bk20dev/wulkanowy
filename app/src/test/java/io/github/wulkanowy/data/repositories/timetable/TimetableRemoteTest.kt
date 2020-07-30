package io.github.wulkanowy.data.repositories.timetable

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.getStudentEntity
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Timetable
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime.now

class TimetableRemoteTest {

    @SpyK
    private var mockSdk = Sdk()

    @MockK
    private lateinit var semesterMock: Semester

    private val student = getStudentEntity()

    @Before
    fun initApi() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getTimetableTest() {
        coEvery {
            mockSdk.getTimetable(
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        } returns listOf(
            getTimetable(of(2018, 9, 10)),
            getTimetable(of(2018, 9, 17))
        )

        every { semesterMock.studentId } returns 1
        every { semesterMock.diaryId } returns 1
        every { semesterMock.schoolYear } returns 2019
        every { semesterMock.semesterId } returns 1
        every { mockSdk.switchDiary(any(), any()) } returns mockSdk

        val timetable = runBlocking {
            TimetableRemote(mockSdk).getTimetable(student, semesterMock,
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        }
        assertEquals(2, timetable.size)
    }

    private fun getTimetable(date: LocalDate): Timetable {
        return Timetable(
            date = date,
            number = 0,
            teacherOld = "",
            subjectOld = "",
            roomOld = "",
            subject = "",
            teacher = "",
            group = "",
            canceled = false,
            changes = false,
            info = "",
            room = "",
            end = now(),
            start = now(),
            studentPlan = true
        )
    }
}
