package com.adama.api.service.excel;

import java.io.InputStream;
import java.util.List;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.util.service.AdamaServiceInterface;

public interface ExcelServiceInterface {
	/**
	 * Create Excel file from object list, need the entity of the object in the
	 * list
	 * 
	 * @param objectList
	 * @param name
	 *            of entity
	 * @return {@link InputStream}
	 * @throws {@link ExcelException}:
	 */
	public <T> InputStream createExcel(List<T> objectList, String entityName) throws ExcelException;

	/**
	 * Read Excel file and if no id will create a new object, if have id it will
	 * update the object of entity type
	 * 
	 * @param inputStream
	 * @param entityType
	 * @param service
	 * @param mapStringNameList
	 *            (the name of the Map<String,String> for this object
	 * @throws {@link ExcelException}:
	 */
	public <T extends DeleteEntityAbstract, R extends AdamaMongoRepository<T, String>> void readExcel(InputStream inputStream, Class<T> entityType, AdamaServiceInterface<T> service,
			List<String> mapStringNameList) throws ExcelException;
}