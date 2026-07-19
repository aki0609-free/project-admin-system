package com.project.backend.schedule.tax.services;

import java.io.InputStream;
import java.net.URI;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.tax.entity.IncomeTaxBracket;
import com.project.backend.features.tax.repository.IncomeTaxBracketRepository;
import com.project.backend.schedule.tax.parser.IncomeTaxParser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncomeTaxUpdateService {

    private final IncomeTaxBracketRepository incomeTaxBracketRepository;

    @SuppressWarnings("null")
    public void updateIncomeTax(String fileUrl, IncomeTaxParser parser) throws Exception {
        int year = Year.now().getValue();

        try (InputStream inputStream = new URI(fileUrl).toURL().openStream()) {
            List<IncomeTaxBracket> brackets = parser.parse(inputStream, year);
            incomeTaxBracketRepository.deleteByYear(year);
            incomeTaxBracketRepository.saveAll(brackets);
            System.out.println("IncomeTaxBracket updated for year " + year);
        }
    }
    
}
