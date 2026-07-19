package com.project.backend.schedule.tax.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.project.backend.features.tax.entity.IncomeTaxBracket;

public class ExcelIncomeTaxParser implements IncomeTaxParser {

    @Override
    public List<IncomeTaxBracket> parse(InputStream inputStream, int year) throws Exception {
        List<IncomeTaxBracket> brackets = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() == 0) continue;

                int minSalary = (int) row.getCell(0).getNumericCellValue();
                int maxSalary = (int) row.getCell(1).getNumericCellValue();
                int dependents = (int) row.getCell(2).getNumericCellValue();
                int taxAmount = (int) row.getCell(3).getNumericCellValue();

                IncomeTaxBracket bracket = new IncomeTaxBracket();
                bracket.setMinSalary(minSalary);
                bracket.setMaxSalary(maxSalary);
                bracket.setDependents(dependents);
                bracket.setTaxAmount(taxAmount);
                bracket.setYear(year);
                brackets.add(bracket);
            }
        }
        return brackets;
    }
    
    
}
