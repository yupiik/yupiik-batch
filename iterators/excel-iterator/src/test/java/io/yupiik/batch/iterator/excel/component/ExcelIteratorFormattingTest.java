/*
 * Copyright (c) 2021-present - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.batch.iterator.excel.component;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the cell rendering (formulas, dates, decimals, blanks) of the default {@link ExcelIterator.ListMapper}
 * since it relies on POI {@code DataFormatter}/{@code FormulaEvaluator} which can change between POI versions.
 */
class ExcelIteratorFormattingTest {
    @TempDir
    private Path work;

    @ParameterizedTest
    @ValueSource(strings = {"xls", "xlsx"})
    void formatting(final String type) throws IOException {
        final var file = work.resolve("formatting." + type);
        write(file, "xls".equals(type) ? HSSFWorkbook::new : XSSFWorkbook::new);

        final var defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US); // DataFormatter uses the default locale
        try (final var it = ExcelIterator.ofLines(file, 1)) {
            final var lines = new ArrayList<List<String>>();
            it.forEachRemaining(lines::add);
            assertEquals(List.of(
                    List.of("label", "number", "decimal", "date", "formula", "text formula"),
                    List.of("a", "10", "1.50", "2021-06-01", "20", "a-10"),
                    List.of(), // missing row
                    List.of("b", "", "-3.25", "", "0", "b-")
            ), lines);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    private void write(final Path file, final Supplier<Workbook> factory) throws IOException {
        try (final var workbook = factory.get(); final var out = Files.newOutputStream(file)) {
            workbook.createSheet("ignored").createRow(0).createCell(0).setCellValue("not read");

            final var sheet = workbook.createSheet("data");
            final var decimal = workbook.createCellStyle();
            decimal.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            final var date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));

            final var header = sheet.createRow(0);
            final var titles = List.of("label", "number", "decimal", "date", "formula", "text formula");
            for (int i = 0; i < titles.size(); i++) {
                header.createCell(i).setCellValue(titles.get(i));
            }

            final var first = sheet.createRow(1);
            first.createCell(0).setCellValue("a");
            first.createCell(1).setCellValue(10);
            first.createCell(2).setCellValue(1.5);
            first.getCell(2).setCellStyle(decimal);
            first.createCell(3).setCellValue(LocalDateTime.of(2021, 6, 1, 0, 0));
            first.getCell(3).setCellStyle(date);
            first.createCell(4).setCellFormula("B2*2");
            first.createCell(5).setCellFormula("A2&\"-\"&B2");

            // row index 2 is not created at all

            final var second = sheet.createRow(3);
            second.createCell(0).setCellValue("b");
            // B4 and D4 are not created
            second.createCell(2).setCellValue(-3.25);
            second.getCell(2).setCellStyle(decimal);
            second.createCell(4).setCellFormula("B4*2");
            second.createCell(5).setCellFormula("A4&\"-\"&B4");

            workbook.write(out);
        }
    }
}
