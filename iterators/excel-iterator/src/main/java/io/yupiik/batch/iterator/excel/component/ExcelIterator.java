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

import io.yupiik.batch.runtime.documentation.IteratorDescription;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@IteratorDescription("Reads an excel file sheet row by row.")
public class ExcelIterator<A> implements Iterator<A>, AutoCloseable {
    private final RowMapper<A> mapper;
    private final Workbook workbook;
    private final FormulaEvaluator evaluator;
    private final DataFormatter formatter;
    private final Sheet sheet;
    private final int lastRowNum;
    private int currentRow = 0;

    public ExcelIterator(final Path file, final int sheetIndex, final RowMapper<A> mapper) {
        this.mapper = mapper;
        try {
            this.workbook = WorkbookFactory.create(Files.newInputStream(file));
        } catch (final IOException ioException) {
            throw new IllegalArgumentException(ioException);
        }
        this.evaluator = this.workbook.getCreationHelper().createFormulaEvaluator();
        this.formatter = new DataFormatter(true);
        this.sheet = this.workbook.getSheetAt(sheetIndex);
        this.lastRowNum = sheet.getLastRowNum();
    }

    @Override
    public boolean hasNext() {
        return currentRow <= lastRowNum;
    }

    @Override
    public A next() {
        final var a = mapper.apply(sheet.getRow(currentRow), evaluator, formatter);
        currentRow++;
        return a;
    }

    @Override
    public void close() {
        try {
            workbook.close();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @IteratorDescription.Factory("Creates a new `ExcelIterator` with a custom row mapper.")
    public static <A> ExcelIterator<A> of(final Path file, final int sheetIndex, final RowMapper<A> mapper) {
        return new ExcelIterator<>(file, sheetIndex, mapper);
    }

    @IteratorDescription.Factory("Creates a new `ExcelIterator` with a default row mapper mapping lines as `List<String>`.")
    public static ExcelIterator<List<String>> ofLines(final Path file, final int sheetIndex) {
        return new ExcelIterator<>(file, sheetIndex, new ListMapper());
    }

    public static class ListMapper implements RowMapper<List<String>> {
        @Override
        public List<String> apply(final Row row, final FormulaEvaluator evaluator, final DataFormatter formatter) {
            return row == null ? List.of() : IntStream.range(0, row.getLastCellNum())
                    .mapToObj(row::getCell)
                    .map(it -> {
                        if (it == null) {
                            return "";
                        }
                        if (it.getCellType() != CellType.FORMULA) {
                            return formatter.formatCellValue(it);
                        }
                        return formatter.formatCellValue(it, evaluator);
                    })
                    .collect(toList());
        }

        public static ListMapper of() {
            return new ListMapper();
        }
    }

    public interface RowMapper<A> {
        A apply(Row row, FormulaEvaluator evaluator, DataFormatter formatter);
    }
}
