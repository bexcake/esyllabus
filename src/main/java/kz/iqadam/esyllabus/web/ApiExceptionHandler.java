package kz.iqadam.esyllabus.web;

import java.util.LinkedHashMap;
import kz.iqadam.esyllabus.syllabus.service.SyllabusReviewValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SyllabusReviewValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleSyllabusReviewValidation(
            SyllabusReviewValidationException exception
    ) {
        var metrics = exception.metrics();
        var details = new LinkedHashMap<String, Object>();
        details.put("progress", metrics.progress());
        details.put("sectionsCompleted", metrics.sectionsCompleted());
        details.put("sectionsTotal", metrics.sectionsTotal());
        details.put("missingSections", metrics.missingSections());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(
                        "SYLLABUS_INCOMPLETE",
                        exception.getMessage(),
                        details
                ));
    }
}
