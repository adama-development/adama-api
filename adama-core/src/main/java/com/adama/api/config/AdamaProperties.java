package com.adama.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import lombok.Data;

/**
 * Properties specific to Adama.
 *
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "adama", ignoreUnknownFields = false)
public class AdamaProperties {
	private final Http http = new Http();
	private final Mail mail = new Mail();
	private final Security security = new Security();
	private final Async async = new Async();
	private final Swagger swagger = new Swagger();
	private final CorsConfiguration cors = new CorsConfiguration();
	private final S3Configuration s3 = new S3Configuration();
	private final IonicConfiguration ionic = new IonicConfiguration();

	@Data
	public static class Http {
		private final Cache cache = new Cache();

		@Data
		public static class Cache {
			private int timeToLiveInDays = 1461;
		}
	}

	@Data
	public static class Mail {
		private String from = "contact@adama-development.com";
		private String error = "contact@adama-development.com";
	}

	@Data
	public static class Security {
		private final Authentication authentication = new Authentication();
		private String defaultFirstLogin = "admin";
		private String defaultFirstPassword = "admin";

		@Data
		public static class Authentication {
			private final Jwt jwt = new Jwt();

			@Data
			public static class Jwt {
				private String secret;
				private String externalLoginSecret;
				private long tokenValidityInSeconds = 1800;
				private long tokenValidityInSecondsForRememberMe = 604800;
				private long refreshTokenValidityInSeconds = 31449600;
			}
		}
	}

	@Data
	public static class Async {
		private int corePoolSize = 2;
		private int maxPoolSize = 50;
		private int queueCapacity = 10000;
	}

	@Data
	public static class Swagger {
		private String title = "adama_API API";
		private String description = "adama_API API documentation";
		private String version = "0.0.1";
		private String termsOfServiceUrl;
		private String contactName;
		private String contactUrl;
		private String contactEmail;
		private String license;
		private String licenseUrl;
	}

	@Data
	public static class S3Configuration {
		private String bucket;
		private String accessKey;
		private String secretKey;
		private long urlValidityInSeconds = 600;
	}

	@Data
	public static class IonicConfiguration {
		private String apiKey;
		private String profile;
	}
}
