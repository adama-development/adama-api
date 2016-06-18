package com.adama.api.service.file.abs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.util.StreamUtils;

import com.adama.api.config.AdamaProperties;
import com.adama.api.domain.file.AdamaFileAbstract;
import com.adama.api.repository.file.AdamaFileRepositoryInterface;
import com.adama.api.service.file.AdamaFileServiceInterface;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

public abstract class AdamaFileServiceAbstract<A extends AdamaFileAbstract, R extends AdamaFileRepositoryInterface<A>> implements AdamaFileServiceInterface<A> {
	private AdamaFileRepositoryInterface<A> repo;
	private AdamaProperties adamaProperties;

	@PostConstruct
	public abstract void init();

	@Override
	public A findOne(String id) {
		A entity = repo.findOne(id);
		return entity;
	}

	@Override
	public URL getFileUrl(A adamaFile) throws UnsupportedEncodingException {
		if (adamaFile != null && adamaFile.getFolder() != null && adamaFile.getFileName() != null) {
			AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(adamaProperties.getS3().getAccessKey(), adamaProperties.getS3().getSecretKey()));
			Date expiredDate = Date.from(ZonedDateTime.now().plusSeconds(adamaProperties.getS3().getUrlValidityInSeconds()).toInstant());
			String fileKey = URLDecoder.decode(adamaFile.getFolder(), "UTF-8") + "/" + URLDecoder.decode(adamaFile.getFileName(), "UTF-8");
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(adamaProperties.getS3().getBucket(), fileKey);
			generatePresignedUrlRequest.setMethod(HttpMethod.GET);
			generatePresignedUrlRequest.setExpiration(expiredDate);
			if (adamaFile.getIsPublic()) {
				client.setObjectAcl(adamaProperties.getS3().getBucket(), fileKey, CannedAccessControlList.PublicRead);
				URL url = client.getUrl(adamaProperties.getS3().getBucket(), fileKey);
				return url;
			} else {
				URL url = client.generatePresignedUrl(generatePresignedUrlRequest);
				return url;
			}
		} else {
			return null;
		}
	}

	@Override
	public A saveFile(A adamaFile, InputStream inputStream) {
		AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(adamaProperties.getS3().getAccessKey(), adamaProperties.getS3().getSecretKey()));
		ObjectMetadata metadata = new ObjectMetadata();
		if (adamaFile.getSize() != null && adamaFile.getSize() != 0 && adamaFile.getSize() != -1) {
			metadata.setContentLength(adamaFile.getSize());
		} else {
			try {
				byte[] contentBytes = StreamUtils.copyToByteArray(inputStream);
				Long contentLength = Long.valueOf(contentBytes.length);
				metadata.setContentLength(contentLength);
				inputStream = new ByteArrayInputStream(contentBytes);
			} catch (IOException e) {
				return null;
			}
		}
		metadata.setContentType(adamaFile.getContentType());
		PutObjectResult result;
		if (adamaFile.getIsPublic()) {
			result = client.putObject(new PutObjectRequest(adamaProperties.getS3().getBucket(), adamaFile.getFolder() + "/" + adamaFile.getFileName(), inputStream, metadata)
					.withCannedAcl(CannedAccessControlList.PublicRead));
		} else {
			result = client.putObject(new PutObjectRequest(adamaProperties.getS3().getBucket(), adamaFile.getFolder() + "/" + adamaFile.getFileName(), inputStream, metadata));
		}
		if (result == null) {
			return null;
		}
		return repo.save(adamaFile);
	}

	@Override
	public InputStream getFileInputStream(A adamaFile) throws UnsupportedEncodingException {
		AmazonS3Client client = new AmazonS3Client(new BasicAWSCredentials(adamaProperties.getS3().getAccessKey(), adamaProperties.getS3().getSecretKey()));
		String fileKey = URLDecoder.decode(adamaFile.getFolder(), "UTF-8") + "/" + URLDecoder.decode(adamaFile.getFileName(), "UTF-8");
		S3Object s3Object = client.getObject(adamaProperties.getS3().getBucket(), fileKey);
		return s3Object.getObjectContent();
	}

	public void setRepo(AdamaFileRepositoryInterface<A> repo) {
		this.repo = repo;
	}

	public void setAdamaProperties(AdamaProperties adamaProperties) {
		this.adamaProperties = adamaProperties;
	}
}
