package com.project.backend.schedule.tax.parser;

import java.io.InputStream;
import java.util.List;

import com.project.backend.features.tax.entity.IncomeTaxBracket;

public interface IncomeTaxParser {

    List<IncomeTaxBracket> parse(InputStream inputStream, int year) throws Exception;
    
}
