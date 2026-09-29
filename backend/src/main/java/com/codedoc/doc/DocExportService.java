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
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}
