package com.adama.api.config;

import java.util.Arrays;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.MimeMappings;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Slf4j
@Configuration
public class WebConfigurer implements ServletContextInitializer, EmbeddedServletContainerCustomizer {
	@Inject
	private Environment env;
	@Inject
	private AdamaProperties adamaProperties;

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		log.info("Web application configuration, using profiles: {}", Arrays.toString(env.getActiveProfiles()));
		log.info("Web application fully configured");
	}

	/**
	 * Set up Mime types and, if needed, set the document root.
	 */
	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
		mappings.add("html", "text/html;charset=utf-8");
		mappings.add("json", "text/html;charset=utf-8");
		container.setMimeMappings(mappings);
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = adamaProperties.getCors();
		if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
			source.registerCorsConfiguration("/**", config);
			source.registerCorsConfiguration("/v2/api-docs", config);
		}
		return new CorsFilter(source);
	}
}
