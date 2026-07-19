package com.project.backend.schedule.tax.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.project.backend.features.tax.entity.IncomeTaxBracket;

public class PdfIncomeTaxParser implements IncomeTaxParser {

    @Override
    public List<IncomeTaxBracket> parse(InputStream inputStream, int year) throws Exception {
                List<IncomeTaxBracket> brackets = new ArrayList<>();
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] lines = text.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;

                try {
                    int minSalary = Integer.parseInt(parts[0].replace(",", ""));
                    int maxSalary = Integer.parseInt(parts[2].replace(",", ""));
                    int dependents = Integer.parseInt(parts[3]);
                    int taxAmount = Integer.parseInt(parts[4].replace(",", ""));

                    IncomeTaxBracket bracket = new IncomeTaxBracket();
                    bracket.setMinSalary(minSalary);
                    bracket.setMaxSalary(maxSalary);
                    bracket.setDependents(dependents);
                    bracket.setTaxAmount(taxAmount);
                    bracket.setYear(year);
                    brackets.add(bracket);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return brackets;
    }

}
