package com.adama.api.config.apidoc;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;

import com.adama.api.config.AdamaProperties;
import com.google.common.base.Predicates;

import lombok.extern.slf4j.Slf4j;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Springfox Swagger configuration.
 *
 */
@Slf4j
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

	/**
	 * Swagger Springfox configuration.
	 *
	 * @param adamaProperties
	 *            the properties of the application
	 * @return the Swagger Springfox configuration
	 */
	@Bean
	public Docket swaggerSpringfoxDocket(AdamaProperties adamaProperties) {
		log.debug("Starting Swagger");
		StopWatch watch = new StopWatch();
		watch.start();
		Contact contact = new Contact(adamaProperties.getSwagger().getContactName(),
				adamaProperties.getSwagger().getContactUrl(), adamaProperties.getSwagger().getContactEmail());

		ApiInfo apiInfo = new ApiInfo(adamaProperties.getSwagger().getTitle(),
				adamaProperties.getSwagger().getDescription(), adamaProperties.getSwagger().getVersion(),
				adamaProperties.getSwagger().getTermsOfServiceUrl(), contact, adamaProperties.getSwagger().getLicense(),
				adamaProperties.getSwagger().getLicenseUrl());

		Docket docket = new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo).forCodeGeneration(true)
				.genericModelSubstitutes(ResponseEntity.class).ignoredParameterTypes(Pageable.class)
				.useDefaultResponseMessages(false).directModelSubstitute(java.time.ZonedDateTime.class, Date.class)
				.directModelSubstitute(java.time.LocalDateTime.class, Date.class).select()
				.apis(Predicates.not(RequestHandlerSelectors.basePackage("org.springframework.boot")))				
				.paths(regex("/.*")).build();

		watch.stop();
		log.debug("Started Swagger in {} ms", watch.getTotalTimeMillis());
		return docket;
	}

}
