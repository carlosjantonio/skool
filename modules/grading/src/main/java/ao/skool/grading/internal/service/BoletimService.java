package ao.skool.grading.internal.service;

import ao.skool.academic_structure.api.AcademicStructureQuery;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.grading.internal.web.dto.GradeDtos.StudentGradeSummary;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Generates a boletim (report card) PDF in pt-AO. Layout matches the Angolan
 * convention: subject rows, T1/T2/T3/Média columns, trimester averages, final
 * average, signature block.
 */
@Service
public class BoletimService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);

    private static final Color HEADER_BG = new Color(11, 110, 79); // matches app theme
    private static final Color HEADER_FG = Color.WHITE;

    private final GradeService grades;
    private final AcademicStructureQuery structure;
    private final TenantContext tenant;

    public BoletimService(GradeService grades, AcademicStructureQuery structure, TenantContext tenant) {
        this.grades = grades;
        this.structure = structure;
        this.tenant = tenant;
    }

    @Transactional(readOnly = true)
    public byte[] generate(UUID studentId, UUID academicYearId) {
        StudentGradeSummary summary = grades.summaryFor(studentId, academicYearId);
        String schoolName = structure.findSchool(tenant.current().value())
                .map(AcademicStructureQuery.SchoolView::name)
                .orElse("Escola");

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc, schoolName);
        addStudentBlock(doc, summary);
        addGradesTable(doc, summary);
        addTrimesterAveragesTable(doc, summary);
        addFinalAverage(doc, summary);
        addSignatures(doc);

        doc.close();
        return out.toByteArray();
    }

    private void addHeader(Document doc, String schoolName) {
        Paragraph school = new Paragraph(schoolName, TITLE);
        school.setAlignment(Element.ALIGN_CENTER);
        doc.add(school);

        Paragraph title = new Paragraph("BOLETIM DE NOTAS", SUBTITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(14);
        doc.add(title);

        Paragraph republic = new Paragraph("República de Angola · Ministério da Educação", SMALL);
        republic.setAlignment(Element.ALIGN_CENTER);
        republic.setSpacingAfter(20);
        doc.add(republic);
    }

    private void addStudentBlock(Document doc, StudentGradeSummary s) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(labelValue("Aluno:", s.studentName()));
        table.addCell(labelValue("Ano Lectivo:", s.academicYearName()));
        table.setSpacingAfter(16);
        doc.add(table);
    }

    private PdfPCell labelValue(String label, String value) {
        Phrase p = new Phrase();
        p.add(new com.lowagie.text.Chunk(label + " ", BODY_BOLD));
        p.add(new com.lowagie.text.Chunk(value == null ? "-" : value, BODY));
        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        return cell;
    }

    private void addGradesTable(Document doc, StudentGradeSummary s) {
        PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 1.5f, 1.5f, 1.5f});
        table.setWidthPercentage(100);

        addHeaderRow(table, "Disciplina", "1º Trim.", "2º Trim.", "3º Trim.", "Média");

        // Group averages by subject
        Map<UUID, String> subjectNames = new LinkedHashMap<>();
        Map<UUID, Map<String, BigDecimal>> grid = new LinkedHashMap<>();
        TreeSet<String> trimesters = new TreeSet<>();
        for (var tsa : s.subjectAverages()) {
            subjectNames.putIfAbsent(tsa.subjectId(), tsa.subjectName());
            grid.computeIfAbsent(tsa.subjectId(), k -> new LinkedHashMap<>())
                    .put(tsa.trimesterKey(), tsa.average());
            trimesters.add(tsa.trimesterKey());
        }

        for (var entry : subjectNames.entrySet()) {
            UUID subjectId = entry.getKey();
            String subjectName = entry.getValue();
            Map<String, BigDecimal> byTrim = grid.getOrDefault(subjectId, Map.of());

            table.addCell(bodyCell(subjectName, Element.ALIGN_LEFT));
            BigDecimal sum = BigDecimal.ZERO;
            int present = 0;
            for (String trim : List.of("T1", "T2", "T3")) {
                BigDecimal v = byTrim.get(trim);
                table.addCell(bodyCell(v == null ? "-" : v.toPlainString(), Element.ALIGN_CENTER));
                if (v != null) { sum = sum.add(v); present++; }
            }
            String avg = present == 0 ? "-" : sum.divide(BigDecimal.valueOf(present), 2, java.math.RoundingMode.HALF_UP).toPlainString();
            table.addCell(bodyCell(avg, Element.ALIGN_CENTER));
        }
        table.setSpacingAfter(14);
        doc.add(table);
    }

    private void addTrimesterAveragesTable(Document doc, StudentGradeSummary s) {
        PdfPTable table = new PdfPTable(new float[]{4f, 1.5f, 1.5f, 1.5f});
        table.setWidthPercentage(100);
        addHeaderRow(table, "Média por Trimestre", "1º", "2º", "3º");
        table.addCell(bodyCell("Média Geral", Element.ALIGN_LEFT));
        for (String trim : List.of("T1", "T2", "T3")) {
            BigDecimal v = s.trimesterAverages().get(trim);
            table.addCell(bodyCell(v == null ? "-" : v.toPlainString(), Element.ALIGN_CENTER));
        }
        table.setSpacingAfter(14);
        doc.add(table);
    }

    private void addFinalAverage(Document doc, StudentGradeSummary s) {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk("Média Final: ", SUBTITLE));
        p.add(new com.lowagie.text.Chunk(s.finalAverage().toPlainString() + " / 20", TITLE));
        p.setAlignment(Element.ALIGN_RIGHT);
        p.setSpacingAfter(28);
        doc.add(p);
    }

    private void addSignatures(Document doc) {
        String today = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
                .withLocale(new Locale("pt", "AO")));
        Paragraph date = new Paragraph("Data: " + today, BODY);
        date.setSpacingAfter(28);
        doc.add(date);

        PdfPTable signatures = new PdfPTable(3);
        signatures.setWidthPercentage(100);
        signatures.addCell(signatureCell("Director"));
        signatures.addCell(signatureCell("Encarregado de Educação"));
        signatures.addCell(signatureCell("Aluno"));
        doc.add(signatures);
    }

    private PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell(new Phrase("________________________\n" + label, BODY));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(30);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void addHeaderRow(PdfPTable table, String... cells) {
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_FG)));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private PdfPCell bodyCell(String value, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value, BODY));
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
