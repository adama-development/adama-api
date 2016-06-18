package com.adama.api.service.file;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.adama.api.domain.file.AdamaFileAbstract;

/**
 * Interface for managing the file
 *
 */
public interface AdamaFileServiceInterface<A extends AdamaFileAbstract> {
	/**
	 * Get the "id" adamaFile.
	 * 
	 * @param id
	 *            the id of the entity
	 * @return the entity
	 */
	A findOne(String id);

	/**
	 * Get expiring url for the adamaFile
	 * 
	 * @param adamaFile
	 *            the file for generate the url
	 * @return the URL with expired time
	 */
	public URL getFileUrl(A adamaFile) throws UnsupportedEncodingException;

	/**
	 * Save the file on disk and save object AdamaFile to reference it
	 * 
	 * @param adamaFile
	 *            the information where to save the file
	 * @param inputStream
	 *            the stream to save in the file
	 * 
	 * @return the file save in AdamaFile
	 */
	public A saveFile(A adamaFile, InputStream inputStream);

	/**
	 * Get the inputStream from an AdamaFile
	 * 
	 * @param adamaFile
	 *            the file to retrieve
	 * 
	 * @return the inputStream for the AdamaFile
	 * @throws UnsupportedEncodingException
	 */
	public InputStream getFileInputStream(A adamaFile) throws UnsupportedEncodingException;
}