package io.github.wulkanowy.ui.modules.grade

import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.GradeRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.data.repositories.SemesterRepository
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.ALL_YEAR
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.BOTH_SEMESTERS
import io.github.wulkanowy.ui.modules.grade.GradeAverageMode.ONE_SEMESTER
import io.github.wulkanowy.utils.calcAverage
import io.github.wulkanowy.utils.changeModifier
import io.github.wulkanowy.utils.flowWithResourceIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(FlowPreview::class)
class GradeAverageProvider @Inject constructor(
    private val semesterRepository: SemesterRepository,
    private val gradeRepository: GradeRepository,
    private val preferencesRepository: PreferencesRepository
) {

    private val plusModifier get() = preferencesRepository.gradePlusModifier

    private val minusModifier get() = preferencesRepository.gradeMinusModifier

    fun getGradesDetailsWithAverage(student: Student, semesterId: Int, forceRefresh: Boolean) =
        flowWithResourceIn {
            val semesters = semesterRepository.getSemesters(student)

            when (preferencesRepository.gradeAverageMode) {
                ONE_SEMESTER -> getGradeSubjects(
                    student = student,
                    semester = semesters.single { it.semesterId == semesterId },
                    forceRefresh = forceRefresh
                )
                BOTH_SEMESTERS -> calculateCombinedAverage(
                    student = student,
                    semesters = semesters,
                    semesterId = semesterId,
                    forceRefresh = forceRefresh,
                    averageMode = BOTH_SEMESTERS
                )
                ALL_YEAR -> calculateCombinedAverage(
                    student = student,
                    semesters = semesters,
                    semesterId = semesterId,
                    forceRefresh = forceRefresh,
                    averageMode = ALL_YEAR
                )
            }
        }.distinctUntilChanged()

    private fun calculateCombinedAverage(
        student: Student,
        semesters: List<Semester>,
        semesterId: Int,
        forceRefresh: Boolean,
        averageMode: GradeAverageMode
    ): Flow<Resource<List<GradeSubject>>> {
        val gradeAverageForceCalc = preferencesRepository.gradeAverageForceCalc
        val selectedSemester = semesters.single { it.semesterId == semesterId }
        val firstSemester =
            semesters.single { it.diaryId == selectedSemester.diaryId && it.semesterName == 1 }

        val selectedSemesterGradeSubjects =
            getGradeSubjects(student, selectedSemester, forceRefresh)

        if (selectedSemester == firstSemester) return selectedSemesterGradeSubjects

        val firstSemesterGradeSubjects =
            getGradeSubjects(student, firstSemester, forceRefresh)

        return selectedSemesterGradeSubjects.combine(firstSemesterGradeSubjects) { secondSemesterGradeSubject, firstSemesterGradeSubject ->
            if (firstSemesterGradeSubject.status == Status.ERROR) {
                return@combine firstSemesterGradeSubject
            }

            val isAnyAverage = secondSemesterGradeSubject.data.orEmpty().any { it.average != .0 }
            val updatedData = secondSemesterGradeSubject.data?.map { secondSemesterSubject ->
                val firstSemesterSubject = firstSemesterGradeSubject.data.orEmpty()
                    .singleOrNull { it.subject == secondSemesterSubject.subject }

                val updatedAverage = if (averageMode == ALL_YEAR) {
                    calculateAllYearAverage(
                        student = student,
                        isAnyAverage = isAnyAverage,
                        gradeAverageForceCalc = gradeAverageForceCalc,
                        secondSemesterSubject = secondSemesterSubject,
                        firstSemesterSubject = firstSemesterSubject
                    )
                } else {
                    calculateBothSemestersAverage(
                        student = student,
                        isAnyAverage = isAnyAverage,
                        gradeAverageForceCalc = gradeAverageForceCalc,
                        secondSemesterSubject = secondSemesterSubject,
                        firstSemesterSubject = firstSemesterSubject
                    )
                }
                secondSemesterSubject.copy(average = updatedAverage)
            }
            secondSemesterGradeSubject.copy(data = updatedData)
        }
    }

    private fun calculateAllYearAverage(
        student: Student,
        isAnyAverage: Boolean,
        gradeAverageForceCalc: Boolean,
        secondSemesterSubject: GradeSubject,
        firstSemesterSubject: GradeSubject?
    ) = if (!isAnyAverage || gradeAverageForceCalc) {
        val updatedSecondSemesterGrades =
            secondSemesterSubject.grades.updateModifiers(student)
        val updatedFirstSemesterGrades =
            firstSemesterSubject?.grades?.updateModifiers(student).orEmpty()

        (updatedSecondSemesterGrades + updatedFirstSemesterGrades).calcAverage()
    } else {
        secondSemesterSubject.average
    }

    private fun calculateBothSemestersAverage(
        student: Student,
        isAnyAverage: Boolean,
        gradeAverageForceCalc: Boolean,
        secondSemesterSubject: GradeSubject,
        firstSemesterSubject: GradeSubject?
    ) = if (!isAnyAverage || gradeAverageForceCalc) {
        val secondSemesterAverage =
            secondSemesterSubject.grades.updateModifiers(student).calcAverage()
        val firstSemesterAverage = firstSemesterSubject?.grades?.updateModifiers(student)
            ?.calcAverage() ?: secondSemesterAverage
        val divider = if (secondSemesterSubject.grades.any { it.weightValue > .0 }) 2 else 1

        (secondSemesterAverage + firstSemesterAverage) / divider
    } else {
        (secondSemesterSubject.average + (firstSemesterSubject?.average ?: secondSemesterSubject.average)) / 2
    }

    private fun getGradeSubjects(
        student: Student,
        semester: Semester,
        forceRefresh: Boolean
    ): Flow<Resource<List<GradeSubject>>> {
        val gradeAverageForceCalc = preferencesRepository.gradeAverageForceCalc

        return gradeRepository.getGrades(student, semester, forceRefresh = forceRefresh)
            .map { res ->
                val (details, summaries) = res.data ?: null to null
                val isAnyAverage = summaries.orEmpty().any { it.average != .0 }
                val allGrades = details.orEmpty().groupBy { it.subject }

                val items = summaries?.emulateEmptySummaries(
                    student,
                    semester,
                    allGrades.toList(),
                    isAnyAverage
                )?.map { summary ->
                    val grades = allGrades[summary.subject].orEmpty()
                    GradeSubject(
                        subject = summary.subject,
                        average = if (!isAnyAverage || gradeAverageForceCalc) {
                            grades.updateModifiers(student).calcAverage()
                        } else summary.average,
                        points = summary.pointsSum,
                        summary = summary,
                        grades = grades
                    )
                }

                Resource(res.status, items, res.error)
            }
    }

    private fun List<GradeSummary>.emulateEmptySummaries(
        student: Student,
        semester: Semester,
        grades: List<Pair<String, List<Grade>>>,
        calcAverage: Boolean
    ): List<GradeSummary> {
        if (isNotEmpty() && size > grades.size) return this

        return grades.mapIndexed { i, (subject, details) ->
            singleOrNull { it.subject == subject }?.let { return@mapIndexed it }
            GradeSummary(
                studentId = student.studentId,
                semesterId = semester.semesterId,
                position = i,
                subject = subject,
                predictedGrade = "",
                finalGrade = "",
                proposedPoints = "",
                finalPoints = "",
                pointsSum = "",
                average = if (calcAverage) details.updateModifiers(student).calcAverage() else .0
            )
        }
    }

    private fun List<Grade>.updateModifiers(student: Student): List<Grade> {
        return if (student.loginMode == Sdk.Mode.SCRAPPER.name) {
            map { it.changeModifier(plusModifier, minusModifier) }
        } else this
    }
}
