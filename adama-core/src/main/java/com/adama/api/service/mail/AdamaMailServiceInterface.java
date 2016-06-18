package com.adama.api.service.mail;

import javax.servlet.http.HttpServletRequest;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

public interface AdamaMailServiceInterface<U extends AdamaUser<? extends DeleteEntityAbstract>> {

	/**
	 * send email when account is created for asking password
	 * 
	 * @param user
	 * @param baseUrl
	 */
	public abstract void sendCreationEmail(U user, String baseUrl, String serverUrl);

	/**
	 * send email with token for reset password
	 * 
	 * @param user
	 * @param baseUrl
	 */
	public abstract void sendPasswordResetMail(U user, String baseUrl, String serverUrl);

	/**
	 * send email with given parameters
	 * 
	 * @param to
	 * @param subject
	 * @param content
	 * @param isMultipart
	 * @param isHtml
	 */
	public abstract void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml);

	/**
	 *
	 * Send mail to error address when exception occurs
	 * 
	 * @param e
	 *            the exception
	 * @param request
	 *            the request when the error occurs
	 */
	public void sendErrorEmail(Exception e, HttpServletRequest request);
}