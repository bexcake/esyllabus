package kz.iqadam.esyllabus.requests.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kz.iqadam.esyllabus.requests.api.LibraryRequestResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibraryRequestExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] exportSingleForm(LibraryRequestResponse request) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Request form");
            sheet.setDefaultColumnWidth(18);
            sheet.setColumnWidth(1, 18_000);

            var headerStyle = createHeaderStyle(workbook);
            var wrapStyle = createWrapStyle(workbook);
            var bodyStyle = createBodyStyle(workbook);

            sheet.createRow(1).createCell(1).setCellValue("Департамент: " + safe(request.department()));
            sheet.createRow(2).createCell(1).setCellValue("Уровень подготовки: " + safe(request.educationLevel()));
            sheet.createRow(3).createCell(1).setCellValue("Дата: " + DATE_FORMATTER.format(request.requestDate()));

            var headerRow = sheet.createRow(5);
            var headers = List.of(
                    "№",
                    "Название книги, автор, ISBN, издательство, год издания",
                    "Дисциплина",
                    "ОП",
                    "Курс",
                    "Триместр",
                    "Кол-во по заявкам",
                    "Вид литературы"
            );

            for (int index = 0; index < headers.size(); index++) {
                var cell = headerRow.createCell(index);
                cell.setCellValue(headers.get(index));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 6;
            for (var item : request.items()) {
                var row = sheet.createRow(rowIndex++);
                setCell(row, 0, item.lineNumber(), bodyStyle);
                setCell(row, 1, formatBookDescription(item), wrapStyle);
                setCell(row, 2, item.discipline(), bodyStyle);
                setCell(row, 3, item.educationalProgram(), bodyStyle);
                setCell(row, 4, item.courseNumber(), bodyStyle);
                setCell(row, 5, item.trimester(), bodyStyle);
                setCell(row, 6, item.quantity(), bodyStyle);
                setCell(row, 7, item.literatureType(), bodyStyle);
            }

            rowIndex += 2;
            var noteRow = sheet.createRow(rowIndex);
            var noteCell = noteRow.createCell(1);
            noteCell.setCellValue("""
                    Просим указать не менее 3 наименований учебной литературы на каждую дисциплину по следующим видам литературы:

                    Учебная литература / course textbook
                    Учебно-методическая литература / educational and methodological literature
                    Научная литература / scientific literature
                    """);
            noteCell.setCellStyle(wrapStyle);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to build request form export");
        }
    }

    public byte[] exportRequestsRegistry(List<LibraryRequestResponse> requests) {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Requests");
            var headerStyle = createHeaderStyle(workbook);
            var bodyStyle = createBodyStyle(workbook);

            var header = sheet.createRow(0);
            var headers = List.of(
                    "Request ID",
                    "Requester",
                    "School",
                    "Department",
                    "Education level",
                    "Status",
                    "Director",
                    "Expected month",
                    "Library feedback",
                    "Updated at",
                    "Items"
            );
            for (int index = 0; index < headers.size(); index++) {
                var cell = header.createCell(index);
                cell.setCellValue(headers.get(index));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, 6_000);
            }

            int rowIndex = 1;
            for (var request : requests) {
                var row = sheet.createRow(rowIndex++);
                setCell(row, 0, request.id(), bodyStyle);
                setCell(row, 1, request.requesterName() + " (" + request.requesterUsername() + ")", bodyStyle);
                setCell(row, 2, request.schoolName(), bodyStyle);
                setCell(row, 3, request.department(), bodyStyle);
                setCell(row, 4, request.educationLevel(), bodyStyle);
                setCell(row, 5, request.status(), bodyStyle);
                setCell(row, 6, request.directorUsername(), bodyStyle);
                setCell(row, 7, safe(request.expectedPurchaseMonth()), bodyStyle);
                setCell(row, 8, safe(request.libraryFeedback()), bodyStyle);
                setCell(row, 9, request.updatedAt().toString(), bodyStyle);
                setCell(row, 10, request.items().size(), bodyStyle);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to build requests export");
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createWrapStyle(XSSFWorkbook workbook) {
        var style = createBodyStyle(workbook);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private void setCell(org.apache.poi.ss.usermodel.Row row, int columnIndex, Object value, CellStyle style) {
        var cell = row.createCell(columnIndex);
        cell.setCellValue(String.valueOf(value));
        cell.setCellStyle(style);
    }

    private String formatBookDescription(LibraryRequestResponse.ItemResponse item) {
        return String.join(", ", List.of(
                item.title(),
                safe(item.author()),
                safe(item.isbn()),
                safe(item.publisher()),
                safe(item.publicationYear())
        ).stream().filter(text -> !text.isBlank()).toList());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
