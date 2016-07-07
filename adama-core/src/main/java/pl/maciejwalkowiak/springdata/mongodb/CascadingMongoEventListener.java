package pl.maciejwalkowiak.springdata.mongodb;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * This class aims to simplify cascade saving for Mongo DB.
 * 
 * Taken from
 * https://www.javacodegeeks.com/2013/11/spring-data-mongodb-cascade-save-on-
 * dbref-objects.html
 */
@SuppressWarnings("rawtypes")
@Component
@Slf4j
public class CascadingMongoEventListener extends AbstractMongoEventListener {
	@Inject
	private MongoOperations mongoOperations;

	@Override
	public void onBeforeConvert(final Object source) {
		ReflectionUtils.doWithFields(source.getClass(), new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				ReflectionUtils.makeAccessible(field);
				if (field.isAnnotationPresent(DBRef.class) && field.isAnnotationPresent(CascadeSave.class)) {
					log.debug("Cascade saving for {}", field.getName());
					final Object fieldValue = field.get(source);
					DbRefFieldCallback callback = new DbRefFieldCallback();
					ReflectionUtils.doWithFields(fieldValue.getClass(), callback);
					if (!callback.isIdFound()) {
						throw new MappingException("Cannot perform cascade save on child object without id set");
					}
					mongoOperations.save(fieldValue);
				}
			}
		});
	}

	private static class DbRefFieldCallback implements ReflectionUtils.FieldCallback {
		private boolean idFound;

		@Override
		public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
			ReflectionUtils.makeAccessible(field);
			if (field.isAnnotationPresent(Id.class)) {
				idFound = true;
			}
		}

		public boolean isIdFound() {
			return idFound;
		}
	}
}