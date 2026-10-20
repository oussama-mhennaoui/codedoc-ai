package com.codedoc.quality;

import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class QualityExportService {

    public String toMarkdown(QualityReport report) {
        StringBuilder sb = new StringBuilder();
        String filename = report.getSourceFile() != null ? report.getSourceFile().getFilename() : "Fichier inconnu";
        JsonNode analysis = report.getRawAnalysis();

        sb.append("# Rapport d'Analyse de Qualité & Code Smells\n\n");
        sb.append("**Fichier :** `").append(filename).append("`  \n");
        sb.append("**Date :** ").append(report.getCreatedAt()).append("  \n\n");

        sb.append("## 1. Score Global & Dette Technique\n\n");
        sb.append("- **Score de Qualité Global :** **").append(report.getScoreGlobal()).append(" / 100**\n");
        sb.append("- **Niveau de dette technique :** ").append(report.getTechnicalDebtRating().toUpperCase()).append("\n");
        sb.append("- **Temps de refactoring estimé :** ").append(report.getTechnicalDebtMinutes()).append(" minutes\n");

        if (analysis != null) {
            JsonNode detteNode = analysis.path("dette_technique");
            if (detteNode.has("resume")) {
                sb.append("- **Synthèse dette :** ").append(detteNode.path("resume").asText()).append("\n");
            }

            JsonNode compNode = analysis.path("complexite");
            sb.append("\n## 2. Métriques de Complexité & Duplication\n\n");
            sb.append("- **Complexité cyclomatique :** ").append(compNode.path("score_cyclomatique").asInt(0)).append("\n");
            sb.append("- **Charge cognitive :** ").append(compNode.path("score_cognitif").asInt(0)).append("\n");
            sb.append("- **Détails complexité :** ").append(compNode.path("details").asText("")).append("\n");

            JsonNode dupNode = analysis.path("duplication");
            sb.append("- **Taux de duplication estimé :** ").append(dupNode.path("pourcentage_estime").asInt(0)).append("%\n");
            sb.append("- **Détails duplication :** ").append(dupNode.path("details").asText("")).append("\n\n");

            JsonNode solidNode = analysis.path("respect_solid");
            sb.append("## 3. Évaluation des Principes SOLID\n\n");
            sb.append("- **SRP (Single Responsibility) :** ").append(solidNode.path("srp").asText("N/A")).append("\n");
            sb.append("- **OCP (Open/Closed) :** ").append(solidNode.path("ocp").asText("N/A")).append("\n");
            sb.append("- **LSP (Liskov Substitution) :** ").append(solidNode.path("lsp").asText("N/A")).append("\n");
            sb.append("- **ISP (Interface Segregation) :** ").append(solidNode.path("isp").asText("N/A")).append("\n");
            sb.append("- **DIP (Dependency Inversion) :** ").append(solidNode.path("dip").asText("N/A")).append("\n");
            sb.append("- **Synthèse SOLID :** ").append(solidNode.path("resume").asText("N/A")).append("\n\n");

            JsonNode smellsNode = analysis.path("code_smells");
            sb.append("## 4. Code Smells Détectés & Refactoring Priorisé\n\n");
            if (smellsNode.isArray() && !smellsNode.isEmpty()) {
                sb.append("| Gravité | Ligne | Type | Description | Suggestion de Refactoring |\n");
                sb.append("| :--- | :--- | :--- | :--- | :--- |\n");

                List<JsonNode> sortedSmells = new ArrayList<>();
                smellsNode.forEach(sortedSmells::add);
                sortedSmells.sort(Comparator.comparingInt(this::severityWeight).reversed());

                for (JsonNode smell : sortedSmells) {
                    sb.append("| ")
                      .append(smell.path("gravite").asText("").toUpperCase()).append(" | ")
                      .append(smell.path("ligne").asInt(0)).append(" | ")
                      .append(smell.path("type").asText("").replace("|", "\\|")).append(" | ")
                      .append(smell.path("description").asText("").replace("|", "\\|")).append(" | ")
                      .append(smell.path("suggestion_refactoring").asText("").replace("|", "\\|")).append(" |\n");
                }

                boolean hasPatches = sortedSmells.stream().anyMatch(s -> s.hasNonNull("code_refactorise") && !s.path("code_refactorise").asText().isBlank());
                if (hasPatches) {
                    sb.append("\n### Patchs de Refactoring Recommandés\n\n");
                    for (JsonNode smell : sortedSmells) {
                        String refactored = smell.path("code_refactorise").asText("");
                        if (!refactored.isBlank()) {
                            sb.append("#### ").append(smell.path("type").asText("")).append(" (Ligne ").append(smell.path("ligne").asInt(0)).append(")\n\n");
                            String original = smell.path("code_original").asText("");
                            if (!original.isBlank()) {
                                sb.append("**Avant (Code problématique) :**\n```\n").append(original).append("\n```\n\n");
                            }
                            sb.append("**Après (Refactoring proposé) :**\n```\n").append(refactored).append("\n```\n\n");
                        }
                    }
                }
            } else {
                sb.append("Aucun code smell détecté. Le code respecte les bonnes pratiques.\n");
            }
        }

        return sb.toString();
    }

    public byte[] toPdf(QualityReport report) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 41, 59));
            Font sectionFont = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(51, 65, 85));
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(30, 41, 59));
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(30, 41, 59));
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(71, 85, 105));

            String filename = report.getSourceFile() != null ? report.getSourceFile().getFilename() : "Fichier";

            // Title
            Paragraph title = new Paragraph("Rapport de Qualité de Code & Code Smells", titleFont);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph subtitle = new Paragraph("Fichier analysé : " + filename + " | Score : " + report.getScoreGlobal() + "/100", boldFont);
            subtitle.setSpacingAfter(14f);
            document.add(subtitle);

            // Summary Table
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(14f);

            addTableCell(summaryTable, "Score Global", boldFont, new Color(241, 245, 249));
            addTableCell(summaryTable, report.getScoreGlobal() + " / 100", boldFont, getScoreColor(report.getScoreGlobal()));
            addTableCell(summaryTable, "Dette Technique", boldFont, new Color(241, 245, 249));
            addTableCell(summaryTable, report.getTechnicalDebtMinutes() + " min (" + report.getTechnicalDebtRating() + ")", normalFont, Color.WHITE);

            document.add(summaryTable);

            JsonNode analysis = report.getRawAnalysis();
            if (analysis != null) {
                // Section Complexité & Duplication
                document.add(new Paragraph("1. Synthèse Métriques & Dette", sectionFont));
                JsonNode complexite = analysis.path("complexite");
                JsonNode duplication = analysis.path("duplication");
                JsonNode dette = analysis.path("dette_technique");

                Paragraph metricsP = new Paragraph(
                        "• Complexité Cyclomatique : " + complexite.path("score_cyclomatique").asInt(0) +
                        " | Charge Cognitive : " + complexite.path("score_cognitif").asInt(0) + "\n" +
                        "• Taux de Duplication estimé : " + duplication.path("pourcentage_estime").asInt(0) + "%\n" +
                        "• Synthèse Dette : " + dette.path("resume").asText(""),
                        normalFont
                );
                metricsP.setSpacingAfter(10f);
                document.add(metricsP);

                // Section SOLID
                document.add(new Paragraph("2. Respect des Principes SOLID", sectionFont));
                JsonNode solid = analysis.path("respect_solid");
                Paragraph solidP = new Paragraph(
                        "• SRP : " + solid.path("srp").asText("") + "\n" +
                        "• OCP : " + solid.path("ocp").asText("") + "\n" +
                        "• LSP : " + solid.path("lsp").asText("") + "\n" +
                        "• ISP : " + solid.path("isp").asText("") + "\n" +
                        "• DIP : " + solid.path("dip").asText("") + "\n" +
                        "• Synthèse : " + solid.path("resume").asText(""),
                        normalFont
                );
                solidP.setSpacingAfter(12f);
                document.add(solidP);

                // Section Code Smells
                document.add(new Paragraph("3. Code Smells Détectés & Suggestions de Refactoring", sectionFont));
                JsonNode smellsNode = analysis.path("code_smells");

                if (smellsNode.isArray() && !smellsNode.isEmpty()) {
                    PdfPTable smellsTable = new PdfPTable(new float[]{1.5f, 1f, 2f, 3.5f, 4f});
                    smellsTable.setWidthPercentage(100);
                    smellsTable.setSpacingBefore(8f);

                    addTableHeader(smellsTable, "Gravité", boldFont);
                    addTableHeader(smellsTable, "Ligne", boldFont);
                    addTableHeader(smellsTable, "Type", boldFont);
                    addTableHeader(smellsTable, "Description", boldFont);
                    addTableHeader(smellsTable, "Suggestion Refactoring", boldFont);

                    List<JsonNode> sortedSmells = new ArrayList<>();
                    smellsNode.forEach(sortedSmells::add);
                    sortedSmells.sort(Comparator.comparingInt(this::severityWeight).reversed());

                    for (JsonNode smell : sortedSmells) {
                        String gravite = smell.path("gravite").asText("faible");
                        Color cellColor = getSeverityBgColor(gravite);

                        addTableCell(smellsTable, gravite.toUpperCase(), boldFont, cellColor);
                        addTableCell(smellsTable, String.valueOf(smell.path("ligne").asInt(1)), smallFont, Color.WHITE);
                        addTableCell(smellsTable, smell.path("type").asText(""), smallFont, Color.WHITE);
                        addTableCell(smellsTable, smell.path("description").asText(""), smallFont, Color.WHITE);
                        addTableCell(smellsTable, smell.path("suggestion_refactoring").asText(""), smallFont, Color.WHITE);
                    }

                    document.add(smellsTable);
                } else {
                    Paragraph cleanCode = new Paragraph("Aucun code smell détecté. Le code est propre.", normalFont);
                    cleanCode.setSpacingBefore(6f);
                    document.add(cleanCode);
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Quality PDF report: " + e.getMessage(), e);
        }
    }

    private int severityWeight(JsonNode smell) {
        String gravite = smell.path("gravite").asText("faible").toLowerCase();
        return switch (gravite) {
            case "critique" -> 3;
            case "moyen" -> 2;
            default -> 1;
        };
    }

    private Color getScoreColor(int score) {
        if (score >= 80) return new Color(220, 252, 231); // Light Green
        if (score >= 50) return new Color(254, 243, 199); // Light Yellow
        return new Color(254, 226, 226); // Light Red
    }

    private Color getSeverityBgColor(String gravite) {
        return switch (gravite.toLowerCase()) {
            case "critique" -> new Color(254, 226, 226);
            case "moyen" -> new Color(254, 243, 199);
            default -> new Color(241, 245, 249);
        };
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(226, 232, 240));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        table.addCell(cell);
    }
}
