package com.adama.api.service.mail.abst;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.CharEncoding;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import com.adama.api.config.AdamaProperties;
import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.service.mail.AdamaMailServiceInterface;
import com.adama.api.service.user.AdamaUserServiceInterface;
import com.adama.api.util.http.HttpUtils;
import com.adama.api.util.security.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending e-mails.
 * <p>
 * We use the @Async annotation to send e-mails asynchronously.
 * </p>
 */
@Slf4j
public abstract class AdamaUserMailServiceAbstract<A extends AdamaUser<? extends DeleteEntityAbstract>> implements AdamaMailServiceInterface<A> {
	private AdamaProperties adamaProperties;
	private JavaMailSenderImpl javaMailSender;
	private Environment env;
	private AdamaUserServiceInterface<A> userService;
	private MessageSource messageSource;
	private SpringTemplateEngine templateEngine;

	@PostConstruct
	public abstract void init();

	@Override
	@Async
	public void sendCreationEmail(A user, String baseUrl, String serverUrl) {
		log.debug("Sending creation e-mail to '{}'", user.getEmail());
		Locale locale = Locale.forLanguageTag(user.getLangKey());
		Context context = new Context(locale);
		context.setVariable("user", user);
		context.setVariable("baseUrl", baseUrl);
		context.setVariable("serverUrl", serverUrl);
		String content = templateEngine.process("activationEmail", context);
		String subject = messageSource.getMessage("email.activation.title", null, locale);
		sendEmail(user.getEmail(), subject, content, false, true);
	}

	@Override
	@Async
	public void sendPasswordResetMail(A user, String baseUrl, String serverUrl) {
		log.debug("Sending password reset e-mail to '{}'", user.getEmail());
		Locale locale = Locale.forLanguageTag(user.getLangKey());
		Context context = new Context(locale);
		context.setVariable("user", user);
		context.setVariable("baseUrl", baseUrl);
		context.setVariable("serverUrl", serverUrl);
		String content = templateEngine.process("passwordResetEmail", context);
		String subject = messageSource.getMessage("email.reset.title", null, locale);
		sendEmail(user.getEmail(), subject, content, false, true);
	}

	@Override
	@Async
	public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
		log.debug("Send e-mail[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}", isMultipart, isHtml, to, subject, content);
		// Prepare message using a Spring helper
		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, CharEncoding.UTF_8);
			message.setTo(to);
			message.setFrom(adamaProperties.getMail().getFrom());
			message.setSubject(subject);
			message.setText(content, isHtml);
			javaMailSender.send(mimeMessage);
			log.debug("Sent e-mail to User '{}'", to);
		} catch (Exception e) {
			log.warn("E-mail could not be sent to user '{}', exception is: {}", to, e.getMessage());
		}
	}

	@Override
	@Async
	public void sendErrorEmail(Exception e, HttpServletRequest request) {
		String subject = "[" + env.getProperty("spring.application.name") + "] Application Error: " + request.getRequestURL();
		String emailUser = SecurityUtils.getCurrentUserLogin().map(login -> {
			Optional<A> user = userService.findOneByLogin(login);
			if (user.isPresent()) {
				return user.get().getEmail();
			}
			return "";
		}).orElse("");
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		printWriter.flush();
		String stackTrace = writer.toString();
		String uri = HttpUtils.getUriFromRequest(request);
		String text = "An error occured in your application: " + e + "<br/><br/>For User:  " + emailUser + "<br/><br/><br/>" + "for URL :" + uri + "<br/><br/><br/>" + " for exception: " + stackTrace;
		sendEmail(adamaProperties.getMail().getError(), subject, text, false, true);
	}

	public void setAdamaProperties(AdamaProperties adamaProperties) {
		this.adamaProperties = adamaProperties;
	}

	public void setJavaMailSender(JavaMailSenderImpl javaMailSender) {
		this.javaMailSender = javaMailSender;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}

	public void setUserService(AdamaUserServiceInterface<A> userService) {
		this.userService = userService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setTemplateEngine(SpringTemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}
}
