package com.adama.api.util.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public final class JSR310DateConverters {
	private JSR310DateConverters() {
	}

	public static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
		public static final LocalDateToDateConverter INSTANCE = new LocalDateToDateConverter();

		private LocalDateToDateConverter() {
		}

		public Date convert(LocalDate source) {
			return source == null ? null : Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
		}
	}

	public static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
		public static final DateToLocalDateConverter INSTANCE = new DateToLocalDateConverter();

		private DateToLocalDateConverter() {
		}

		public LocalDate convert(Date source) {
			return source == null ? null : ZonedDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault()).toLocalDate();
		}
	}

	public static class ZonedDateTimeToDateConverter implements Converter<ZonedDateTime, Date> {
		public static final ZonedDateTimeToDateConverter INSTANCE = new ZonedDateTimeToDateConverter();

		private ZonedDateTimeToDateConverter() {
		}

		public Date convert(ZonedDateTime source) {
			return source == null ? null : Date.from(source.toInstant());
		}
	}

	public static class DateToZonedDateTimeConverter implements Converter<Date, ZonedDateTime> {
		public static final DateToZonedDateTimeConverter INSTANCE = new DateToZonedDateTimeConverter();

		private DateToZonedDateTimeConverter() {
		}

		public ZonedDateTime convert(Date source) {
			return source == null ? null : ZonedDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		}
	}

	public static class ZonedDateTimeToStringConverter implements Converter<ZonedDateTime, String> {
		public static final ZonedDateTimeToStringConverter INSTANCE = new ZonedDateTimeToStringConverter();

		private ZonedDateTimeToStringConverter() {
		}

		public String convert(ZonedDateTime source) {
			return source == null ? null : source.toString();
		}
	}

	public static class StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {
		public static final StringToZonedDateTimeConverter INSTANCE = new StringToZonedDateTimeConverter();

		private StringToZonedDateTimeConverter() {
		}

		public ZonedDateTime convert(String source) {
			return source == null ? null : ZonedDateTime.parse(source);
		}
	}

	public static class LocalDateTimeToStringConverter implements Converter<LocalDate, String> {
		public static final LocalDateTimeToStringConverter INSTANCE = new LocalDateTimeToStringConverter();

		private LocalDateTimeToStringConverter() {
		}

		public String convert(LocalDate source) {
			return source == null ? null : source.toString();
		}
	}

	public static class StringToLocalDateTimeConverter implements Converter<String, LocalDate> {
		public static final StringToLocalDateTimeConverter INSTANCE = new StringToLocalDateTimeConverter();

		private StringToLocalDateTimeConverter() {
		}

		public LocalDate convert(String source) {
			return source == null ? null : LocalDate.parse(source);
		}
	}

	public static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
		public static final LocalDateTimeToDateConverter INSTANCE = new LocalDateTimeToDateConverter();

		private LocalDateTimeToDateConverter() {
		}

		public Date convert(LocalDateTime source) {
			return source == null ? null : Date.from(source.atZone(ZoneId.systemDefault()).toInstant());
		}
	}

	public static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
		public static final DateToLocalDateTimeConverter INSTANCE = new DateToLocalDateTimeConverter();

		private DateToLocalDateTimeConverter() {
		}

		public LocalDateTime convert(Date source) {
			return source == null ? null : LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
		}
	}
}
