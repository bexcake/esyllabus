package kz.iqadam.esyllabus.web;

import java.util.Map;

public record ValidationErrorResponse(
        String code,
        String message,
        Map<String, Object> details
) {
}
