package com.s15.codectest;

import static org.junit.Assert.assertEquals;

import com.s15.codectest.export.CsvExporter;

import org.junit.Test;

// Sessions: CsvExporter.csv
public class CsvTest {

    @Test
    public void normalValuesStayTheSame() {
        assertEquals("", CsvExporter.csv(null));
        assertEquals("12", CsvExporter.csv(12));
        assertEquals("COMPLETED", CsvExporter.csv("COMPLETED"));
    }

    @Test
    public void commaGetsQuoted() {
        assertEquals("\"a,b\"", CsvExporter.csv("a,b"));
    }

    @Test
    public void quotesGetDoubled() {
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.csv("say \"hi\""));
    }

    @Test
    public void newLineGetsQuoted() {
        assertEquals("\"line1\nline2\"", CsvExporter.csv("line1\nline2"));
    }
}
