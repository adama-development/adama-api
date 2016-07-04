package com.adama.api.service.excel.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
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
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.service.excel.ExcelServiceInterface;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.excel.util.FormattingHtml;
import com.adama.api.service.util.service.AdamaServiceInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExcelServiceImpl implements ExcelServiceInterface {
	private CellStyle dateCellStyle;
	private CellStyle cellStyle;

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
				ObjectMapper mapper = new ObjectMapper();
				List<Map<String, Object>> listMap = objectList.parallelStream().map(object -> {
					try {
						return JsonFlattener.flattenAsMap(mapper.writeValueAsString(object));
					} catch (JsonProcessingException e) {
						log.error(e.getMessage(), e);
						throw new UncheckedIOException(e.getMessage(), e);
					}
				}).collect(Collectors.toList());
				// we get a KeySet with all the elements of each keySet in
				// alphabetical order
				List<String> headerList = listMap.parallelStream().flatMap(map -> map.keySet().stream()).distinct().sorted((e1, e2) -> e1.compareToIgnoreCase(e2)).collect(Collectors.toList());
				log.info("Header is : {}", headerList);
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
				table_style.setShowColumnStripes(false); // showColumnStripes=0
				table_style.setShowRowStripes(true); // showRowStripes=1
				/* Define the data range including headers */
				AreaReference my_data_range = new AreaReference(new CellReference(0, 0), new CellReference(objectList.size(), headerList.size()));
				/* Set Range to the Table */
				cttable.setRef(my_data_range.formatAsString());
				cttable.setDisplayName(entityName);
				cttable.setName(entityName.toUpperCase());
				cttable.setId(1L);
				CTTableColumns columns = cttable.addNewTableColumns();
				columns.setCount(headerList.size() + 1); // define number of
															// columns
				CTAutoFilter autofilter = cttable.addNewAutoFilter();
				for (int i = 0; i < headerList.size() + 1; i++) {
					CTTableColumn column = columns.addNewTableColumn();
					column.setName("Column" + i);
					column.setId(i + 1);
					CTFilterColumn filter = autofilter.addNewFilterColumn();
					filter.setColId(i + 1);
					filter.setShowButton(true);
				}
				for (int i = 0; i <= headerList.size(); i++) {
					entitySheet.autoSizeColumn(i);
					// Include width of drop down button
					if (entitySheet.getColumnWidth(i) < 65000) {
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
	public <T extends DeleteEntityAbstract, R extends AdamaMongoRepository<T, String>> void readExcel(InputStream inputStream, Class<T> entityType, AdamaServiceInterface<T> service,
			List<String> mapStringNameList) throws ExcelException {
		Workbook workbook;
		try {
			workbook = WorkbookFactory.create(inputStream);
			Sheet entitySheet = workbook.getSheet(entityType.getSimpleName());
			if (entitySheet == null) {
				throw new ExcelException("Cannot find sheet with name: " + entityType.getSimpleName());
			}
			Row firstRow = entitySheet.getRow(0);
			// find column Id
			Integer columnIdIndex = null;
			for (int cellIter = 0; cellIter <= firstRow.getLastCellNum(); cellIter++) {
				if (firstRow.getCell(cellIter) != null && firstRow.getCell(cellIter).getCellType() == Cell.CELL_TYPE_STRING
						&& firstRow.getCell(cellIter).getStringCellValue().equalsIgnoreCase(entityType.getSimpleName() + "." + DeleteEntityAbstract.ID_FIELD_NAME)) {
					columnIdIndex = cellIter;
					break;
				}
			}
			if (columnIdIndex == null) {
				throw new ExcelException("Cannot find column with name: " + DeleteEntityAbstract.ID_FIELD_NAME);
			}
			for (int rowIndex = 1; rowIndex <= entitySheet.getLastRowNum(); rowIndex++) {
				Row row = entitySheet.getRow(rowIndex);
				// get current object instance
				// if have id we update, if no id we create the object
				String id = null;
				if (row.getCell(columnIdIndex) != null) {
					id = row.getCell(columnIdIndex).getStringCellValue();
				}
				log.info("id " + id);
				T modelInstance = null;
				// Boolean needUpate;
				if (id != null && !id.trim().isEmpty()) {
					modelInstance = service.findOne(id);
				} else {
					modelInstance = entityType.newInstance();
				}
				for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
					if (cellIndex != columnIdIndex) {
						Cell cell = row.getCell(cellIndex);
						String fieldName = null;
						if (firstRow.getCell(cellIndex) != null && firstRow.getCell(cellIndex).getCellType() == Cell.CELL_TYPE_STRING) {
							fieldName = firstRow.getCell(cellIndex).getStringCellValue().replace(entityType.getSimpleName() + ".", "");
						}
						if (fieldName == null) {
							throw new ExcelException("Column " + cellIndex + " cannot be empty or not a string");
						}
						if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
							if (fieldName.contains("String")) {
								fieldName = fieldName.replace("String.", "");
								for (String mapName : mapStringNameList) {
									Field currentField = ReflectionUtils.findField(entityType, mapName);
									@SuppressWarnings("unchecked")
									Map<String, String> objectMap = (Map<String, String>) ReflectionUtils.getField(currentField, modelInstance);
									ReflectionUtils.makeAccessible(currentField);
									objectMap.put(fieldName, cell.getStringCellValue());
									log.info("for the key : " + fieldName + " We add the map: " + objectMap);
									ReflectionUtils.setField(currentField, modelInstance, objectMap);
								}
							} else {
								Field currentField = ReflectionUtils.findField(entityType, fieldName);
								ReflectionUtils.makeAccessible(currentField);
								ReflectionUtils.setField(currentField, modelInstance, cell.getStringCellValue());
							}
						}
					}
				}
				log.info("We save " + modelInstance);
				service.save(modelInstance);
			}
		} catch (EncryptedDocumentException | InvalidFormatException | IOException | InstantiationException | IllegalAccessException e) {
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
		// we lock all the id cell
		// CellStyle unlockedCellStyle = wb.createCellStyle();
		// unlockedCellStyle.setLocked(false);
		// for (int i = 0; i <= entitySheet.getLastRowNum(); i++) {
		// XSSFRow row = entitySheet.getRow(i);
		// for (int j = 0; j <= row.getLastCellNum(); j++) {
		// if (i != 0 && (firstRow.getCell(j) != null &&
		// !firstRow.getCell(j).getStringCellValue().equals(Model.ID_FIELDNAME)))
		// {
		// XSSFCell cell = row.getCell(j);
		// if (row.getCell(j) != null) {
		// cell.setCellStyle(unlockedCellStyle);
		// }
		// }
		// }
		// }
		// entitySheet.lockAutoFilter(false);
		// entitySheet.lockSort(false);
		// entitySheet.lockInsertRows(false);
		// entitySheet.enableLocking();
	}

	private <T> void writeRow(T object, int numberOfColumn, Row rowToAddEntity, XSSFSheet entitySheet) throws IllegalArgumentException, IllegalAccessException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> objectMap = JsonFlattener.flattenAsMap(mapper.writeValueAsString(object));
		for (int i = 0; i < numberOfColumn; i++) {
			Cell cell = rowToAddEntity.createCell(i);
			Row firstRow = entitySheet.getRow(0);
			Cell cellTop = firstRow.getCell(i);
			Object value = objectMap.get(cellTop.getStringCellValue());
			if (value != null) {
				fillFieldWithObject(value, cell);
			} else {
				fillFieldWithObject("", cell);
			}
		}
	}

	private void fillFieldWithObject(Object value, Cell cell) throws IllegalArgumentException, IllegalAccessException {
		log.info("Object " + value);
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
		if (value instanceof Integer) {
			cell.setCellValue((Integer) value);
		}
		if (value instanceof String) {
			String stringValue = (String) value;
			Document doc = Jsoup.parse(stringValue);
			FormattingHtml formatter = new FormattingHtml();
			NodeTraversor traversor = new NodeTraversor(formatter);
			traversor.traverse(doc);
			RichTextString richTextString = new XSSFRichTextString(formatter.toString());
			cell.setCellValue(richTextString);
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
	// private String createFieldName(Field field, Integer index) {
	// if (index == null) {
	// // return field.getDeclaringClass().getSimpleName() + "." +
	// // field.getName();
	// return field.getName() ;
	// }
	// // return field.getDeclaringClass().getSimpleName() + "." +
	// // field.getName() + index;
	// return field.getName() + index;
	// }
	//
	// public class FieldWithObject {
	// public Object object;
	// public Field field;
	// public String fieldName;
	//
	// public FieldWithObject(Object object, Field field, String fieldName) {
	// this.object = object;
	// this.field = field;
	// this.fieldName = fieldName;
	// }
	// }
}
