package com.codedoc.audit;

import com.codedoc.audit.dto.SecurityAuditDto;
import com.codedoc.audit.dto.VulnerabilityDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class SecurityAuditExportService {

        public byte[] toPdf(SecurityAuditDto audit) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        Document document = new Document();
                        PdfWriter.getInstance(document, baos);
                        document.open();

                        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
                        Font scoreFont = new Font(Font.HELVETICA, 16, Font.BOLD);
                        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
                        Font contentFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
                        Font severityFont = new Font(Font.HELVETICA, 12, Font.BOLD);

                        document.add(new Paragraph("Security Audit Report", titleFont));
                        document.add(
                                        new Paragraph("File: "
                                                        + (audit.getFileName() != null ? audit.getFileName() : "N/A"),
                                                        contentFont));
                        document.add(new Paragraph(
                                        "Date: " + (audit.getCreatedAt() != null ? audit.getCreatedAt().toString()
                                                        : "N/A"),
                                        contentFont));
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph(
                                        "Security Score: "
                                                        + (audit.getSecurityScore() != null ? audit.getSecurityScore()
                                                                        : "N/A"),
                                        scoreFont));
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph("Summary", sectionFont));
                        document.add(new Paragraph(
                                        audit.getSummary() != null ? audit.getSummary() : "No summary available.",
                                        contentFont));
                        document.add(new Paragraph("\n"));

                        int vulnSize = audit.getVulnerabilities() != null ? audit.getVulnerabilities().size() : 0;
                        document.add(new Paragraph("Vulnerabilities Detected (" + vulnSize + ")", sectionFont));
                        document.add(new Paragraph("\n"));

                        if (audit.getVulnerabilities() != null) {
                                for (VulnerabilityDto v : audit.getVulnerabilities()) {
                                        document.add(new Paragraph(v.getType() != null ? v.getType() : "Unknown Type",
                                                        severityFont));
                                        document.add(new Paragraph(
                                                        "Severity: " + (v.getSeverity() != null
                                                                        ? v.getSeverity().toString()
                                                                        : "UNKNOWN"),
                                                        contentFont));
                                        document.add(new Paragraph("Location: "
                                                        + (v.getFileName() != null ? v.getFileName() : "N/A")
                                                        + " (Line " + (v.getLine() != null ? v.getLine() : "N/A") + ")",
                                                        contentFont));
                                        if (v.getCweId() != null && !v.getCweId().isEmpty()) {
                                                document.add(new Paragraph("CWE: " + v.getCweId(), contentFont));
                                        }
                                        document.add(new Paragraph(
                                                        "Description: " + (v.getDescription() != null
                                                                        ? v.getDescription()
                                                                        : "No description"),
                                                        contentFont));
                                        document.add(new Paragraph(
                                                        "Consequences: " + (v.getConsequences() != null
                                                                        ? v.getConsequences()
                                                                        : "N/A"),
                                                        contentFont));
                                        document.add(new Paragraph(
                                                        "Recommended Fix: " + (v.getFix() != null ? v.getFix() : "N/A"),
                                                        contentFont));
                                        document.add(new Paragraph("\n"));
                                        document.add(new Paragraph("--------------------------------------------------",
                                                        contentFont));
                                        document.add(new Paragraph("\n"));
                                }
                        }

                        // Add glossary section at the end
                        document.add(new Paragraph("\n"));
                        document.add(new Paragraph("Lexique de Sécurité", sectionFont));
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph("CWE (Common Weakness Enumeration) :", severityFont));
                        document.add(new Paragraph(
                                        "Dictionnaire universel des faiblesses logicielles permettant d'identifier précisément chaque type de faille.",
                                        contentFont));
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph("OWASP Top 10 :", severityFont));
                        document.add(new Paragraph(
                                        "Standard mondial listant les 10 risques de sécurité les plus critiques pour les applications web.",
                                        contentFont));
                        document.add(new Paragraph("\n"));

                        document.add(new Paragraph("Score de Sécurité (A-F) :", severityFont));
                        document.add(new Paragraph(
                                        "Note globale de sécurité. A est le niveau le plus sûr, F indique une présence de failles critiques.",
                                        contentFont));
                        document.add(new Paragraph("\n"));

                        document.close();
                        return baos.toByteArray();
                } catch (Exception e) {
                        throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
                }
        }
}
