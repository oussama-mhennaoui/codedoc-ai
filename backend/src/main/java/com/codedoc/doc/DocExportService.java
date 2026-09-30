package com.codedoc.doc;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.List;

@Service
public class DocExportService {

    public String toMarkdown(GeneratedDoc doc) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(doc.getSourceFile().getFilename()).append("\n\n");
        
        if (doc.getSections() != null) {
            List<DocSection> sections = doc.getSections().stream()
                .sorted(Comparator.comparing(DocSection::getOrderIndex))
                .toList();
                
            for (DocSection section : sections) {
                sb.append("## ").append(section.getTitle()).append("\n\n");
                sb.append(section.getContent()).append("\n\n");
            }
        }
        
        return sb.toString();
    }

    public byte[] toPdf(GeneratedDoc generatedDoc) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font contentFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

            document.add(new Paragraph(generatedDoc.getSourceFile().getFilename(), titleFont));
            document.add(new Paragraph("\n"));

            if (generatedDoc.getSections() != null) {
                List<DocSection> sections = generatedDoc.getSections().stream()
                    .sorted(Comparator.comparing(DocSection::getOrderIndex))
                    .toList();

                for (DocSection section : sections) {
                    document.add(new Paragraph(section.getTitle(), sectionFont));
                    document.add(new Paragraph("\n"));
                    document.add(new Paragraph(section.getContent(), contentFont));
                    document.add(new Paragraph("\n"));
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    public byte[] toHtml(GeneratedDoc generatedDoc) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>").append(escapeHtml(generatedDoc.getSourceFile().getFilename())).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 900px; margin: 0 auto; padding: 20px; }\n");
        html.append("    h1 { color: #333; border-bottom: 2px solid #3f51b5; padding-bottom: 10px; }\n");
        html.append("    h2 { color: #555; margin-top: 20px; margin-bottom: 10px; }\n");
        html.append("    p { color: #666; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("  <h1>").append(escapeHtml(generatedDoc.getSourceFile().getFilename())).append("</h1>\n");
        
        if (generatedDoc.getSections() != null) {
            List<DocSection> sections = generatedDoc.getSections().stream()
                .sorted(Comparator.comparing(DocSection::getOrderIndex))
                .toList();
            
            for (DocSection section : sections) {
                html.append("  <h2>").append(escapeHtml(section.getTitle())).append("</h2>\n");
                html.append("  <p>").append(escapeHtml(section.getContent())).append("</p>\n");
            }
        }
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString().getBytes();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
