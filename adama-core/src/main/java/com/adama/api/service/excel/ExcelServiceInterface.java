package com.adama.api.service.excel;

import java.io.InputStream;
import java.util.List;

import com.adama.api.service.excel.exception.ExcelException;

public interface ExcelServiceInterface {
	/**
	 * Create Excel file from object list, need the entity of the object in the
	 * list
	 * 
	 * @param objectList
	 * @param name
	 *            of entity
	 * @return {@link InputStream}
	 * @throws {@link
	 *             ExcelException}:
	 */
	public <T> InputStream createExcel(List<T> objectList, String entityName) throws ExcelException;

	/**
	 * Read Excel file and fill the object with it
	 * 
	 * @param inputStream
	 * @param entityType
	 * @return list of entity
	 * @throws {@link
	 *             ExcelException}:
	 */
	public <T> List<T> readExcel(InputStream inputStream, Class<T> entityType, String entityName) throws ExcelException;
}