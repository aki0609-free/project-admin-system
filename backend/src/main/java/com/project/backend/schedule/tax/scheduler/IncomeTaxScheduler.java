package com.project.backend.schedule.tax.scheduler;

import org.apache.poi.util.StringUtil;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.backend.schedule.tax.parser.ExcelIncomeTaxParser;
import com.project.backend.schedule.tax.parser.PdfIncomeTaxParser;
import com.project.backend.schedule.tax.services.IncomeTaxUpdateService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class IncomeTaxScheduler {

    private final IncomeTaxUpdateService updateService;

    @Scheduled(cron = "0 0 0 1 1 ?")
    public void updateIncomeTax() throws Exception {
        String excelUrl = "";
        String pdfUrl = "";
        String fileCategory = "";

        if (StringUtil.endsWithIgnoreCase(fileCategory, "EXCEL")) {
            updateService.updateIncomeTax(excelUrl, new ExcelIncomeTaxParser());
        } else {
            updateService.updateIncomeTax(pdfUrl, new PdfIncomeTaxParser());
        }
    }
    
}
