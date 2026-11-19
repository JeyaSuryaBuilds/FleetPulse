package com.fayzo.fleetpulse.report;

import com.fayzo.fleetpulse.common.ApiException;
import com.fayzo.fleetpulse.common.Authz;
import com.fayzo.fleetpulse.security.Permission;
import com.fayzo.fleetpulse.tenant.TenantContext;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CSV and PDF exports. Requires REPORT_VIEW plus the module permission of the report; data is always limited to the caller's company. */
@RestController
@RequestMapping("/api/v1/reports")
@Transactional(readOnly = true)
public class ReportController {
    private static final Pattern PLACEHOLDER = Pattern.compile(":(c|from|to)\\b");
    private static final int MAX_ROWS = 20000, MAX_PDF_ROWS = 2000;
    private final JdbcTemplate jdbc;
    public ReportController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping
    public List<Map<String, String>> catalogue() {
        Authz.require(Permission.REPORT_VIEW);
        List<Map<String, String>> out = new ArrayList<>();
        ReportDefinitions.ALL.forEach((k, d) -> { if (Authz.has(d.permission())) out.add(Map.of("type", k, "title", d.title())); });
        return out;
    }

    @GetMapping("/{type}")
    public List<Map<String, Object>> json(@PathVariable String type, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return run(type, from, to);
    }

    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@PathVariable String type, @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        List<Map<String, Object>> rows = run(type, from, to);
        ReportDefinitions.Def def = ReportDefinitions.ALL.get(type);
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from, t = to == null ? LocalDate.now() : to;
        String name = "fleetpulse-" + type + "-" + f + "_" + t;
        if (format.equalsIgnoreCase("pdf")) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".pdf\"")
                    .body(pdf(def.title(), rows, f, t));
        }
        if (!format.equalsIgnoreCase("csv")) throw ApiException.badRequest("BAD_FORMAT", "format must be csv or pdf");
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".csv\"")
                .body(csv(rows));
    }

    private List<Map<String, Object>> run(String type, LocalDate from, LocalDate to) {
        ReportDefinitions.Def def = ReportDefinitions.ALL.get(type);
        if (def == null) throw ApiException.notFound("Unknown report");
        Authz.require(Permission.REPORT_VIEW);
        Authz.require(def.permission());
        LocalDate f = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate t = (to == null ? LocalDate.now() : to).plusDays(1);
        if (!t.isAfter(f) || java.time.temporal.ChronoUnit.DAYS.between(f, t) > 1100) throw ApiException.badRequest("BAD_RANGE", "Date range must be positive and at most 3 years");
        long company = TenantContext.require();
        List<Object> args = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(def.sql());
        while (m.find()) args.add(switch (m.group(1)) { case "c" -> company; case "from" -> f; default -> t; });
        String sql = PLACEHOLDER.matcher(def.sql()).replaceAll("?") + " LIMIT " + MAX_ROWS;
        return jdbc.queryForList(sql, args.toArray());
    }

    private static String label(String k) {
        String s = k.replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** Neutralises spreadsheet formula injection (cells starting with = + - @ are prefixed with an apostrophe). */
    private static String csvCell(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0 && !(v instanceof Number)) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private byte[] csv(List<Map<String, Object>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        if (!rows.isEmpty()) {
            sb.append(String.join(",", rows.get(0).keySet().stream().map(ReportController::label).toList())).append("\r\n");
            for (var r : rows) { sb.append(String.join(",", r.values().stream().map(ReportController::csvCell).toList())).append("\r\n"); }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(String title, List<Map<String, Object>> rows, LocalDate from, LocalDate to) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Color orange = new Color(0xFF, 0x87, 0x33), graphite = new Color(0x17, 0x17, 0x17);
            PdfPTable head = new PdfPTable(new float[]{1, 9});
            head.setWidthPercentage(100);
            try {
                Image logo = Image.getInstance(new ClassPathResource("brand/icon-192.png").getInputStream().readAllBytes());
                logo.scaleToFit(34, 34);
                PdfPCell lc = new PdfPCell(logo, false); lc.setBorder(Rectangle.NO_BORDER); head.addCell(lc);
            } catch (Exception e) { PdfPCell lc = new PdfPCell(new Phrase("")); lc.setBorder(Rectangle.NO_BORDER); head.addCell(lc); }
            Paragraph p = new Paragraph("FleetPulse  |  " + title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, graphite));
            p.add(new Chunk("\nPeriod " + from + " to " + to + "   Generated " + java.time.Instant.now(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)));
            PdfPCell tc = new PdfPCell(p); tc.setBorder(Rectangle.BOTTOM); tc.setBorderColor(orange); tc.setBorderWidth(2f); tc.setPaddingBottom(6); head.addCell(tc);
            doc.add(head);
            doc.add(new Paragraph(" "));
            if (rows.isEmpty()) {
                doc.add(new Paragraph("No records for this period.", FontFactory.getFont(FontFactory.HELVETICA, 10)));
            } else {
                List<String> keys = new ArrayList<>(rows.get(0).keySet());
                PdfPTable t = new PdfPTable(keys.size());
                t.setWidthPercentage(100);
                t.setHeaderRows(1);
                for (String k : keys) {
                    PdfPCell c = new PdfPCell(new Phrase(label(k), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                    c.setBackgroundColor(graphite); c.setPadding(4); t.addCell(c);
                }
                int n = 0;
                for (var r : rows) {
                    if (n++ >= MAX_PDF_ROWS) break;
                    for (String k : keys) {
                        Object v = r.get(k);
                        PdfPCell c = new PdfPCell(new Phrase(v == null ? "" : String.valueOf(v), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                        c.setPadding(3); c.setBorderColor(new Color(0xDD, 0xDD, 0xDD)); t.addCell(c);
                    }
                }
                doc.add(t);
                if (rows.size() > MAX_PDF_ROWS) doc.add(new Paragraph("Showing the first " + MAX_PDF_ROWS + " rows. Use CSV export for the full data.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_FAILED", "Could not generate the PDF");
        }
    }
}
