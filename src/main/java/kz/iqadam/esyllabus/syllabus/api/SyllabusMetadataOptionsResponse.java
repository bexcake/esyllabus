package kz.iqadam.esyllabus.syllabus.api;

import java.util.List;
import kz.iqadam.esyllabus.directory.api.DepartmentDirectoryResponse;
import kz.iqadam.esyllabus.directory.api.DirectoryOptionResponse;
import kz.iqadam.esyllabus.directory.api.ProgramDirectoryResponse;
import kz.iqadam.esyllabus.directory.api.SchoolResponse;
import kz.iqadam.esyllabus.directory.api.StaffPickerOptionResponse;

public record SyllabusMetadataOptionsResponse(
        List<StaffPickerOptionResponse> allowedInstructors,
        List<StaffPickerOptionResponse> allowedReviewers,
        List<SchoolResponse> schools,
        List<ProgramDirectoryResponse> programs,
        List<DepartmentDirectoryResponse> departments,
        List<DirectoryOptionResponse> academicYears,
        List<DirectoryOptionResponse> degreeLevels,
        List<DirectoryOptionResponse> courseTypes,
        List<DirectoryOptionResponse> assessmentStages,
        List<DirectoryOptionResponse> trimesters,
        List<DirectoryOptionResponse> languages
) {
}
