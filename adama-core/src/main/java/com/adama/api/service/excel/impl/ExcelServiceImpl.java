package com.adama.api.service.excel.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.service.excel.ExcelServiceInterface;
import com.adama.api.service.excel.annotation.ExcelIgnoreAnnotation;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.excel.util.FormattingHtml;
import com.adama.api.service.util.service.AdamaServiceInterface;

@Service
public class ExcelServiceImpl implements ExcelServiceInterface {

    protected Logger logger;

    public ExcelServiceImpl() {
	this.logger = LoggerFactory.getLogger(getClass());
    }

    private CellStyle dateCellStyle;
    private CellStyle cellStyle;

    private List<FieldWithObject> fieldOkObject = new ArrayList<>();
    private List<FieldWithObject> fieldToReworkObject = new ArrayList<>();

    @Override
    public <T> InputStream createExcel(List<T> objectList, Class<T> entityType) throws ExcelException {

	XSSFWorkbook wb;
	try {
	    wb = getWorkWook(entityType);
	    XSSFSheet entitySheet = wb.getSheet(entityType.getSimpleName());
	    int currentRowIndex = entitySheet.getLastRowNum() + 1;

	    if (objectList != null && objectList.size() != 0) {

		this.dateCellStyle = wb.createCellStyle();
		this.dateCellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("d-mmm-yy"));
		this.dateCellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		this.cellStyle = wb.createCellStyle();
		this.cellStyle.setWrapText(true);
		this.cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		for (T object : objectList) {
		    Row rowToAddEntity = entitySheet.createRow(currentRowIndex);
		    this.fieldOkObject = new ArrayList<>();
		    this.fieldToReworkObject = new ArrayList<>();
		    this.fieldToReworkObject.add(new FieldWithObject((DeleteEntityAbstract) object, ReflectionUtils.findField(entityType, DeleteEntityAbstract.ID_FIELD_NAME), DeleteEntityAbstract.ID_FIELD_NAME));
		    for (Field field : entityType.getDeclaredFields()) {
			this.logger.info("FIELD : " + field.getName());
			this.fieldToReworkObject.add(new FieldWithObject(object, field, this.createFieldName(field, null)));
		    }
		    while (this.fieldToReworkObject.size() != 0) {
			this.sortFieldObject();
		    }
		    writeRow(rowToAddEntity, entitySheet);
		    currentRowIndex++;
		}
		Row firstRow = entitySheet.getRow(0);
		int nbrOfColumn = firstRow.getLastCellNum() - 1;

		// we clean the first row if name is duplicate
		List<String> headerList = new ArrayList<String>();
		for (int i = 0; i <= nbrOfColumn; i++) {
		    Cell cell = firstRow.getCell(i);
		    if (headerList.contains(cell.getStringCellValue())) {
			cell.setCellValue(cell.getStringCellValue() + "." + i);
		    } else {
			headerList.add(cell.getStringCellValue());
		    }
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
		AreaReference my_data_range = new AreaReference(new CellReference(0, 0), new CellReference(objectList.size(), nbrOfColumn));
		/* Set Range to the Table */
		cttable.setRef(my_data_range.formatAsString());
		cttable.setDisplayName("Booking");
		cttable.setName("booking");
		cttable.setId(1L);
		CTTableColumns columns = cttable.addNewTableColumns();
		columns.setCount(nbrOfColumn + 1); // define number of columns
		CTAutoFilter autofilter = cttable.addNewAutoFilter();
		for (int i = 0; i < nbrOfColumn + 1; i++) {
		    CTTableColumn column = columns.addNewTableColumn();
		    column.setName("Column" + i);
		    column.setId(i + 1);
		    CTFilterColumn filter = autofilter.addNewFilterColumn();
		    filter.setColId(i + 1);
		    filter.setShowButton(true);
		}

		for (int i = 0; i <= nbrOfColumn; i++) {
		    entitySheet.autoSizeColumn(i);
		    // Include width of drop down button
		    if (entitySheet.getColumnWidth(i) < 65000) {
			entitySheet.setColumnWidth(i, entitySheet.getColumnWidth(i) + 900);
		    }
		}

	    }
	    return commitChange(wb, entityType);
	} catch (IOException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
	    this.logger.error(e.getMessage(), e);
	    throw new ExcelException(e.getMessage(), e);
	}

    }

    @Override
    public <T extends DeleteEntityAbstract, R extends AdamaMongoRepository<T, String>> void readExcel(InputStream inputStream, Class<T> entityType, AdamaServiceInterface<T> service, List<String> mapStringNameList) throws ExcelException {
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
		if (firstRow.getCell(cellIter) != null && firstRow.getCell(cellIter).getCellType() == Cell.CELL_TYPE_STRING && firstRow.getCell(cellIter).getStringCellValue().equalsIgnoreCase(entityType.getSimpleName() + "." + DeleteEntityAbstract.ID_FIELD_NAME)) {
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
		this.logger.info("id " + id);
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
				    this.logger.info("for the key : " + fieldName + " We add the map: " + objectMap);
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
		this.logger.info("We save " + modelInstance);
		service.save(modelInstance);
	    }
	} catch (EncryptedDocumentException | InvalidFormatException | IOException | InstantiationException | IllegalAccessException e) {
	    this.logger.error(e.getMessage(), e);
	    throw new ExcelException(e.getMessage(), e);
	}

    }

    private <T> XSSFWorkbook getWorkWook(Class<T> entityType) throws IOException {
	ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
	try {
	    // we create the File
	    // we can also use template in resource path
	    XSSFWorkbook wb = new XSSFWorkbook();
	    this.checkWorkBookIntegrity(wb, entityType);
	    wb.write(arrayOutputStream);

	} finally {
	    arrayOutputStream.close();
	}
	InputStream inputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray());
	XSSFWorkbook wb = new XSSFWorkbook(inputStream);
	checkWorkBookIntegrity(wb, entityType);
	return wb;

    }

    private <T> void checkWorkBookIntegrity(XSSFWorkbook wb, Class<T> entityType) {

	// we check if sheets for this model exist, if not we create it
	if (wb.getSheetIndex(entityType.getSimpleName()) == -1) {
	    wb.createSheet(entityType.getSimpleName());
	}

	// we check if each column is correct according to the class parameter
	XSSFSheet entitySheet = wb.getSheet(entityType.getSimpleName());
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

    private <T> void sortFieldObject() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

	List<FieldWithObject> listToSOrt = new ArrayList<FieldWithObject>(this.fieldToReworkObject);
	for (FieldWithObject fieldWithObject : listToSOrt) {
	    ReflectionUtils.makeAccessible(fieldWithObject.field);
	    ExcelIgnoreAnnotation excelIgnore = fieldWithObject.field.getAnnotation(ExcelIgnoreAnnotation.class);
	    if (excelIgnore != null && excelIgnore.value()) {
		this.logger.info("We ignore the field: " + fieldWithObject.field.getName());
	    } else {
		if (!fieldWithObject.field.getType().isAssignableFrom(Boolean.class) && !fieldWithObject.field.getType().isAssignableFrom(Date.class) && !fieldWithObject.field.getType().isAssignableFrom(Double.class) && !fieldWithObject.field.getType().isAssignableFrom(String.class)
			&& !fieldWithObject.field.getType().isAssignableFrom(ZonedDateTime.class) && !fieldWithObject.field.getType().isAssignableFrom(Long.class) && !fieldWithObject.field.getType().isAssignableFrom(Integer.class)) {
		    if (fieldWithObject.field.getType().isAssignableFrom(List.class)) {
			ParameterizedType stringListType = (ParameterizedType) fieldWithObject.field.getGenericType();
			Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
			List<?> objectList = (List<?>) fieldWithObject.field.get(fieldWithObject.object);
			int index = 1;
			for (Object object : objectList) {
			    Boolean alreadyIndexed = false;
			    for (Field field : stringListClass.getDeclaredFields()) {
				ReflectionUtils.makeAccessible(field);
				if (!field.getType().isAssignableFrom(Boolean.class) && !field.getType().isAssignableFrom(Date.class) && !field.getType().isAssignableFrom(Double.class) && !field.getType().isAssignableFrom(String.class) && !field.getType().isAssignableFrom(ZonedDateTime.class)
					&& !field.getType().isAssignableFrom(Long.class) && !field.getType().isAssignableFrom(Integer.class)) {
				    this.fieldToReworkObject.add(new FieldWithObject(object, field, createFieldName(field, index)));
				} else {
				    String fieldName = createFieldName(field, index);
				    if (fieldWithObject.fieldName != null) {
					fieldName = fieldWithObject.fieldName + "." + createFieldName(field, index);
				    }
				    this.fieldOkObject.add(new FieldWithObject(object, field, fieldName));
				    alreadyIndexed = true;
				}
			    }
			    if (alreadyIndexed) {
				index++;
			    }
			}
		    }
		    if (fieldWithObject.field.getType().isAssignableFrom(Map.class)) {
			ParameterizedType stringListType = (ParameterizedType) fieldWithObject.field.getGenericType();
			Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
			if (stringListClass.isAssignableFrom(String.class)) {

			    @SuppressWarnings("unchecked")
			    Map<String, ?> mapList = (Map<String, ?>) fieldWithObject.field.get(fieldWithObject.object);
			    for (String key : mapList.keySet()) {
				this.fieldOkObject.add(new FieldWithObject(mapList.get(key), null, key));
			    }
			}
		    }
		    if (fieldWithObject.field.getGenericType().toString().contains("dragonFly")) {
			// we need go further for this object
			if (!fieldWithObject.field.getGenericType().toString().contains("customizeField")) {
			    Object newObject = fieldWithObject.field.get(fieldWithObject.object);
			    if (newObject != null) {
				for (Field field : fieldWithObject.field.getType().getDeclaredFields()) {
				    String fieldName = createFieldName(field, null);
				    if (fieldWithObject.fieldName != null) {
					fieldName = fieldWithObject.fieldName + "." + createFieldName(field, null);
				    }
				    this.fieldToReworkObject.add(new FieldWithObject(newObject, field, fieldName));
				}
			    }
			}
		    }
		} else {
		    // object ok to display
		    this.fieldOkObject.add(new FieldWithObject(fieldWithObject.object, fieldWithObject.field, this.createFieldName(fieldWithObject.field, null)));
		}
	    }
	    this.fieldToReworkObject.remove(fieldWithObject);
	}
    }

    private <T> void writeRow(Row rowToAddEntity, XSSFSheet entitySheet) throws IllegalArgumentException, IllegalAccessException {
	int index = 0;
	for (FieldWithObject fieldWithObject : this.fieldOkObject) {
	    Cell cell = rowToAddEntity.createCell(index);
	    if (cell == null) {
		cell = rowToAddEntity.createCell(index);
	    }
	    Row firstRow = entitySheet.getRow(0);
	    Cell cellTop = firstRow.getCell(index);
	    if (cellTop == null) {
		cellTop = firstRow.createCell(index);
		cellTop.setCellValue(fieldWithObject.object.getClass().getSimpleName() + "." + fieldWithObject.fieldName);
	    }
	    if (cellTop.getStringCellValue().equals(fieldWithObject.object.getClass().getSimpleName() + "." + fieldWithObject.fieldName)) {
		fillFieldWithObject(fieldWithObject.field, fieldWithObject.object, cell);
	    } else {
		cell.setCellValue("");
	    }
	    index++;
	}
    }

    private void fillFieldWithObject(Field field, Object object, Cell cell) throws IllegalArgumentException, IllegalAccessException {

	this.logger.info("Object " + object);
	this.logger.info("field " + field);
	Object value = object;
	cell.setCellStyle(this.cellStyle);
	if (field != null) {
	    ReflectionUtils.makeAccessible(field);
	    value = field.get(object);
	}
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

    private <T> InputStream commitChange(XSSFWorkbook wb, Class<T> entityType) throws IOException {

	ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
	try {
	    checkWorkBookIntegrity(wb, entityType);
	    wb.write(arrayOutputStream);
	} finally {
	    arrayOutputStream.close();
	}
	return new ByteArrayInputStream(arrayOutputStream.toByteArray());

    }

    private String createFieldName(Field field, Integer index) {
	if (index == null) {
	    // return field.getDeclaringClass().getSimpleName() + "." +
	    // field.getName();
	    return field.getName();
	}
	// return field.getDeclaringClass().getSimpleName() + "." +
	// field.getName() + index;
	return field.getName() + index;
    }

    public class FieldWithObject {
	public Object object;
	public Field field;
	public String fieldName;

	public FieldWithObject(Object object, Field field, String fieldName) {
	    this.object = object;
	    this.field = field;
	    this.fieldName = fieldName;
	}
    }

}
