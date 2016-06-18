package com.adama.api.domain.file;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.mongodb.core.mapping.Field;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An abstract Adama file
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class AdamaFileAbstract extends DeleteEntityAbstract implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String FILE_NAME_FIELD_NAME = "fileName";
	public static final String FOLDER_FIELD_NAME = "folder";
	public static final String SIZE_FIELD_NAME = "size";
	public static final String CONTENT_TYPE_FIELD_NAME = "contentType";
	public static final String IS_PICTURE_FIELD_NAME = "isPicture";
	public static final String IS_PUBLIC_FIELD_NAME = "isPublic";
	/**
	 * The fileName
	 * 
	 */
	@NotNull
	@Size(min = 1, max = 250)
	@Field(FILE_NAME_FIELD_NAME)
	private String fileName;
	/**
	 * the folder name
	 * 
	 */
	@NotNull
	@Size(min = 1, max = 250)
	@Field(FOLDER_FIELD_NAME)
	private String folder;
	/**
	 * the file size
	 * 
	 */
	@NotNull
	@Field(SIZE_FIELD_NAME)
	private Long size;
	/**
	 * the content-type of the file
	 * 
	 */
	@NotNull
	@Field(CONTENT_TYPE_FIELD_NAME)
	private String contentType;
	/**
	 * this file is a picture
	 * 
	 */
	@NotNull
	@Field(IS_PICTURE_FIELD_NAME)
	private Boolean isPicture = false;
	/**
	 * this file can be displayed with public url
	 * 
	 */
	@NotNull
	@Field(IS_PUBLIC_FIELD_NAME)
	private Boolean isPublic = false;
}
