package com.adama.api.service.excel.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.util.AreaReference;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.NodeTraversor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFilterColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.service.excel.ExcelServiceInterface;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.excel.util.FormattingHtml;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExcelServiceImpl implements ExcelServiceInterface {
	private CellStyle dateCellStyle;
	private CellStyle cellStyle;
	@Autowired
	private ObjectMapper mapper;

	@Override
	public <T> InputStream createExcel(List<T> objectList, String entityName) throws ExcelException {
		XSSFWorkbook wb;
		try {
			wb = getWorkWook(entityName);
			XSSFSheet entitySheet = wb.getSheet(entityName);
			if (objectList != null && objectList.size() != 0) {
				// create the style
				this.dateCellStyle = wb.createCellStyle();
				this.dateCellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("d-mmm-yy"));
				this.dateCellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
				this.cellStyle = wb.createCellStyle();
				this.cellStyle.setWrapText(true);
				this.cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
				// create a list of map for each object, each object could have
				// a size of fieldName different
				List<Map<String, Object>> listMap = objectList.parallelStream().map(object -> {
					try {
						return new JsonFlattener(mapper.writeValueAsString(object)).flattenAsMap();
					} catch (JsonProcessingException e) {
						log.error(e.getMessage(), e);
						throw new UncheckedIOException(e.getMessage(), e);
					}
				}).collect(Collectors.toList());
				// we get a KeySet with all the elements of each keySet
				List<String> headerList = listMap.parallelStream().flatMap(map -> map.keySet().stream()).filter(distinctByKey(String::toLowerCase)).sorted(new IdFirstComparator())
						.collect(Collectors.toList());
				// we create the first Row with the keyName
				Row firstRow = entitySheet.getRow(0);
				IntStream.range(0, headerList.size()).forEach(i -> {
					Cell cell = firstRow.createCell(i);
					cell.setCellValue(headerList.get(i));
				});
				// we write the value of each object to the correct column one
				// by one
				int currentRowIndex = 1;
				for (T object : objectList) {
					Row rowToAddEntity = entitySheet.createRow(currentRowIndex);
					writeRow(object, headerList.size(), rowToAddEntity, entitySheet);
					currentRowIndex++;
				}
				/* Create Table into Existing Sheet */
				XSSFTable my_table = entitySheet.createTable();
				/* get CTTable object */
				CTTable cttable = my_table.getCTTable();
				/* Define Styles */
				CTTableStyleInfo table_style = cttable.addNewTableStyleInfo();
				table_style.setName("TableStyleMedium9");
				/* Define Style Options */
				table_style.setShowColumnStripes(false);
				// showColumnStripes=0
				table_style.setShowRowStripes(true);
				// showRowStripes=1
				/* Define the data range including headers */
				AreaReference my_data_range = new AreaReference(new CellReference(0, 0), new CellReference(objectList.size(), headerList.size() - 1));
				/* Set Range to the Table */
				cttable.setRef(my_data_range.formatAsString());
				cttable.setDisplayName(entityName);
				cttable.setName(entityName.toUpperCase());
				cttable.setId(1L);
				CTTableColumns columns = cttable.addNewTableColumns();
				columns.setCount(headerList.size());
				// define number of columns
				CTAutoFilter autofilter = cttable.addNewAutoFilter();
				for (int i = 0; i < headerList.size(); i++) {
					CTTableColumn column = columns.addNewTableColumn();
					column.setName("Column" + i);
					column.setId(i + 1);
					CTFilterColumn filter = autofilter.addNewFilterColumn();
					filter.setColId(i + 1);
					filter.setShowButton(true);
				}
				for (int i = 0; i < headerList.size(); i++) {
					entitySheet.autoSizeColumn(i);
					// Include width of drop down button
					if (entitySheet.getColumnWidth(i) < 60000) {
						entitySheet.setColumnWidth(i, entitySheet.getColumnWidth(i) + 900);
					}
				}
			}
			return commitChange(wb, entityName);
		} catch (IOException | IllegalArgumentException | IllegalAccessException | SecurityException e) {
			log.error(e.getMessage(), e);
			throw new ExcelException(e.getMessage(), e);
		}
	}

	@Override
	public <T> List<T> readExcel(InputStream inputStream, Class<T> entityType, String entityName) throws ExcelException {
		Workbook workbook;
		List<T> entityList = new ArrayList<>();
		try {
			workbook = WorkbookFactory.create(inputStream);
			Sheet entitySheet = workbook.getSheet(entityName);
			if (entitySheet == null) {
				throw new ExcelException("Cannot find sheet with name: " + entityName);
			}
			Row firstRow = entitySheet.getRow(0);
			// find column Id
			Integer columnIdIndex = null;
			for (int cellIter = 0; cellIter <= firstRow.getLastCellNum(); cellIter++) {
				if (firstRow.getCell(cellIter) != null && firstRow.getCell(cellIter).getCellType() == Cell.CELL_TYPE_STRING
						&& firstRow.getCell(cellIter).getStringCellValue().equalsIgnoreCase(DeleteEntityAbstract.ID_FIELD_NAME)) {
					columnIdIndex = cellIter;
					break;
				}
			}
			if (columnIdIndex == null) {
				throw new ExcelException("Cannot find column with name: " + DeleteEntityAbstract.ID_FIELD_NAME);
			}
			for (int rowIndex = 1; rowIndex <= entitySheet.getLastRowNum(); rowIndex++) {
				Row row = entitySheet.getRow(rowIndex);
				// we create a new flatjson from the excel
				ObjectNode flatJson = mapper.createObjectNode();
				// we fill the json object with value column by column
				for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
					Cell cell = row.getCell(cellIndex);
					// get the column name
					String fieldName = firstRow.getCell(cellIndex).getStringCellValue();
					if (fieldName == null) {
						throw new ExcelException("Column " + cellIndex + " cannot be empty or not a string");
					}
					if (cell != null) {
						if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
							flatJson.put(fieldName, cell.getBooleanCellValue());
						}
						if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
							try {
								if (DateUtil.isCellDateFormatted(cell)) {
									// if it's a date
									Date myDate = cell.getDateCellValue();
									ZonedDateTime myZonedDateTime = ZonedDateTime.ofInstant(myDate.toInstant(), ZoneId.of("Z"));
									flatJson.put(fieldName, myZonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
								} else {
									flatJson.put(fieldName, cell.getNumericCellValue());
								}
							} catch (NumberFormatException nfe) {
								// if not a date
								flatJson.put(fieldName, cell.getNumericCellValue());
							}
						}
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							flatJson.put(fieldName, cell.getStringCellValue());
						}
					}
				}
				String jsonUnflatten = JsonUnflattener.unflatten(flatJson.toString());
				T object = mapper.readValue(jsonUnflatten, entityType);
				entityList.add(object);
			}
			return entityList;
		} catch (IOException | EncryptedDocumentException | InvalidFormatException e) {
			log.error(e.getMessage(), e);
			throw new ExcelException(e.getMessage(), e);
		}
	}

	private <T> XSSFWorkbook getWorkWook(String entityName) throws IOException {
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		try {
			// we create the File
			// we can also use template in resource path
			XSSFWorkbook wb = new XSSFWorkbook();
			this.checkWorkBookIntegrity(wb, entityName);
			wb.write(arrayOutputStream);
		} finally {
			arrayOutputStream.close();
		}
		InputStream inputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray());
		XSSFWorkbook wb = new XSSFWorkbook(inputStream);
		checkWorkBookIntegrity(wb, entityName);
		return wb;
	}

	private <T> void checkWorkBookIntegrity(XSSFWorkbook wb, String entityName) {
		// we check if sheets for this model exist, if not we create it
		if (wb.getSheetIndex(entityName) == -1) {
			wb.createSheet(entityName);
		}
		// we check if each column is correct according to the class parameter
		XSSFSheet entitySheet = wb.getSheet(entityName);
		XSSFRow firstRow = entitySheet.getRow(0);
		// if row doesn't exist we create it
		if (firstRow == null) {
			firstRow = entitySheet.createRow(0);
		}
	}

	private <T> void writeRow(T object, int numberOfColumn, Row rowToAddEntity, XSSFSheet entitySheet) throws IllegalArgumentException, IllegalAccessException, JsonProcessingException {
		Map<String, Object> objectMap = new JsonFlattener(mapper.writeValueAsString(object)).flattenAsMap();
		for (int i = 0; i < numberOfColumn; i++) {
			Cell cell = rowToAddEntity.createCell(i);
			Row firstRow = entitySheet.getRow(0);
			Cell cellTop = firstRow.getCell(i);
			Object value = objectMap.get(cellTop.getStringCellValue());
			if (value != null) {
				fillFieldWithObject(value, cell);
			}
		}
	}

	private void fillFieldWithObject(Object value, Cell cell) throws IllegalArgumentException, IllegalAccessException {
		// log.info("Object " + value.getClass() + " -> " + value);
		cell.setCellValue("BUG DURING EXTRACT");
		if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		}
		if (value instanceof Date) {
			cell.setCellValue((Date) value);
			cell.setCellStyle(this.dateCellStyle);
		}
		if (value instanceof ZonedDateTime) {
			cell.setCellValue(((ZonedDateTime) value).toString());
			cell.setCellStyle(this.dateCellStyle);
		}
		if (value instanceof Double) {
			cell.setCellValue((Double) value);
		}
		if (value instanceof Long) {
			cell.setCellValue((Long) value);
		}
		if (value instanceof Long) {
			cell.setCellValue((Long) value);
		}
		if (value instanceof BigDecimal) {
			cell.setCellValue(((BigDecimal) value).longValue());
		}
		if (value instanceof String) {
			try {
				ZonedDateTime myDate = ZonedDateTime.parse((String) value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
				cell.setCellValue(Date.from(myDate.toInstant()));
				cell.setCellStyle(this.dateCellStyle);
			} catch (DateTimeParseException dtpe) {
				String stringValue = (String) value;
				Document doc = Jsoup.parse(stringValue);
				FormattingHtml formatter = new FormattingHtml();
				NodeTraversor traversor = new NodeTraversor(formatter);
				traversor.traverse(doc);
				RichTextString richTextString = new XSSFRichTextString(formatter.toString());
				cell.setCellValue(richTextString);
			}
		}
	}

	private <T> InputStream commitChange(XSSFWorkbook wb, String entityName) throws IOException {
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		try {
			checkWorkBookIntegrity(wb, entityName);
			wb.write(arrayOutputStream);
		} finally {
			arrayOutputStream.close();
		}
		return new ByteArrayInputStream(arrayOutputStream.toByteArray());
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, String> seen = new ConcurrentHashMap<>();
		return t -> seen.put(keyExtractor.apply(t), "") == null;
	}
}

class IdFirstComparator implements Comparator<String> {
	private static List<String> important = Arrays.asList(DeleteEntityAbstract.ID_FIELD_NAME);

	@Override
	public int compare(String o1, String o2) {
		if (important.contains(o1)) {
			return -1;
		}
		if (important.contains(o2)) {
			return 1;
		}
		return o1.compareToIgnoreCase(o2);
	}
}
