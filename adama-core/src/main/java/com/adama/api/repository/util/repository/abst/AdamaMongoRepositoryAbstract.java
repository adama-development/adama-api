package com.adama.api.repository.util.repository.abst;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * Abstract Repository base implementation for Mongo.
 * 
 */
public abstract class AdamaMongoRepositoryAbstract<T extends DeleteEntityAbstract, ID extends Serializable> implements AdamaMongoRepository<T, ID> {
	public final MongoOperations mongoOperations;
	public final MongoEntityInformation<T, ID> entityInformation;

	/**
	 * Creates a new {@link AdamaMongoRepositoryAbstract} for the given
	 * {@link MongoEntityInformation} and {@link MongoTemplate}.
	 * 
	 * @param metadata
	 *            must not be {@literal null}.
	 * @param template
	 *            must not be {@literal null}.
	 */
	public AdamaMongoRepositoryAbstract(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
		Assert.notNull(mongoOperations);
		Assert.notNull(metadata);
		this.entityInformation = metadata;
		this.mongoOperations = mongoOperations;
	}

	@Override
	public <S extends T> S save(S entity) {
		Assert.notNull(entity, "Entity must not be null!");
		if (entityInformation.isNew(entity)) {
			entity.setActive(true);
			mongoOperations.insert(entity, entityInformation.getCollectionName());
		} else {
			mongoOperations.save(entity, entityInformation.getCollectionName());
		}
		return entity;
	}

	@Override
	public <S extends T> List<S> save(Iterable<S> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		List<S> result = convertIterableToList(entities);
		boolean allNew = result.parallelStream().allMatch(entity -> !entityInformation.isNew(entity));
		if (allNew) {
			Stream<S> stream = result.parallelStream().peek(entity -> entity.setActive(true));
			mongoOperations.insertAll(stream.collect(Collectors.toList()));
		} else {
			result.parallelStream().forEach(entity -> save(entity));
		}
		return result;
	}

	@Override
	public abstract T findOne(ID id);

	@Override
	public boolean exists(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		Query query = new Query(getIdCriteria(id));
		Class<T> entityClass = entityInformation.getJavaType();
		String collectionName = entityInformation.getCollectionName();
		return mongoOperations.exists(query, entityClass, collectionName);
	}

	@Override
	public long count() {
		return count(new Query());
	}

	@Override
	public void delete(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		T entity = findOne(id);
		Assert.notNull(entity, "Cannot find the entity with this id!");
		entity.setActive(false);
		save(entity);
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "The given entity must not be null!");
		delete(entityInformation.getId(entity));
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(entity -> delete(entity));
	}

	@Override
	public void deleteAll() {
		mongoOperations.findAll(entityInformation.getJavaType()).parallelStream().forEach(entity -> delete(entity));
	}

	@Override
	public <S extends T> S insert(S entity) {
		Assert.notNull(entity, "Entity must not be null!");
		entity.setActive(true);
		mongoOperations.insert(entity, entityInformation.getCollectionName());
		return entity;
	}

	@Override
	public <S extends T> List<S> insert(Iterable<S> entities) {
		Assert.notNull(entities, "The given Iterable of entities must not be null!");
		List<S> list = convertIterableToList(entities);
		if (!list.isEmpty()) {
			Stream<S> stream = list.parallelStream().peek(entity -> entity.setActive(true));
			mongoOperations.insertAll(stream.collect(Collectors.toList()));
		}
		return list;
	}

	@Override
	public Page<T> search(String key, final Pageable pageable) {
		Field[] allFields = entityInformation.getJavaType().getDeclaredFields();
		List<Criteria> criterias = new ArrayList<>();
		Page<T> result;
		if (key != null && !key.isEmpty()) {
			Assert.notNull(pageable, "pageable must not be null!");
			Arrays.asList(allFields).stream().filter(field -> !ClassUtils.isPrimitiveOrWrapper(field.getType()) && Modifier.isPrivate(field.getModifiers()))
					.forEach(field -> criterias.add(Criteria.where(field.getName()).regex(key, "i")));
			Query query = new Query();
			query.addCriteria(new Criteria().orOperator(criterias.toArray(new Criteria[criterias.size()]))).with(pageable);
			Optional<Query> queryPageable = Optional.ofNullable(query);
			Optional<Sort> sortPageable = Optional.ofNullable(pageable.getSort());
			Optional<Pageable> pageableSort = Optional.ofNullable(pageable);
			List<T> list = findAll(queryPageable, sortPageable, pageableSort);
			Long count = count(query);
			result = new PageImpl<>(list, pageable, count);
		} else {
			Optional<Query> queryPageable = Optional.empty();
			Optional<Pageable> pageableSort = Optional.ofNullable(pageable);
			result = findAllQueryPageable(queryPageable, pageableSort);
		}
		return result;
	}

	@Override
	public T findOne(Optional<Query> query) {
		return query.map(myquery -> mongoOperations.findOne(myquery, entityInformation.getJavaType(), entityInformation.getCollectionName())).orElse(null);
	}

	@Override
	public List<T> findAll(Optional<Query> query) {
		Optional<Query> queryOptional = Optional.empty();
		Optional<Sort> sortOptional = Optional.empty();
		Optional<Pageable> pageableOptional = Optional.empty();
		return findAll(queryOptional, sortOptional, pageableOptional);
	}

	@Override
	public List<T> findAll() {
		Optional<Query> queryOptional = Optional.of(new Query());
		Optional<Sort> sortOptional = Optional.empty();
		Optional<Pageable> pageableOptional = Optional.empty();
		return findAll(queryOptional, sortOptional, pageableOptional);
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		List<ID> parameters = convertIterableToList(ids);
		Optional<Query> queryOptional = Optional.of(new Query(new Criteria(entityInformation.getIdAttribute()).in(parameters)));
		Optional<Sort> sortOptional = Optional.empty();
		Optional<Pageable> pageableOptional = Optional.empty();
		return findAll(queryOptional, sortOptional, pageableOptional);
	}

	@Override
	public Page<T> findAll(final Pageable pageable) {
		// query
		Query query = new Query();
		Optional<Query> queryOptional = Optional.ofNullable(query.with(pageable));
		Optional<Sort> sortOptional = Optional.ofNullable(pageable.getSort());
		Optional<Pageable> pageableOptional = Optional.ofNullable(pageable);
		List<T> list = findAll(queryOptional, sortOptional, pageableOptional);
		// count
		Query queryCount = new Query(getFilterCriteria());
		Long count = count(queryCount);
		// result
		return new PageImpl<>(list, pageable, count);
	}

	@Override
	public List<T> findAll(Sort sort) {
		Optional<Query> queryOptional = Optional.ofNullable(new Query().with(sort));
		Optional<Sort> sortOptional = Optional.ofNullable(sort);
		Optional<Pageable> pageableOptional = Optional.empty();
		return findAll(queryOptional, sortOptional, pageableOptional);
	}

	@Override
	public Page<T> findAllQueryPageable(Optional<Query> queryOptional, Optional<Pageable> pageableOptional) {
		// query
		Optional<Sort> sortOptional = Optional.empty();
		List<T> list = findAll(queryOptional, sortOptional, pageableOptional);
		// count
		Query countQuery = queryOptional.orElse(new Query().addCriteria(getFilterCriteria()));
		Long count = count(countQuery);
		// result
		return new PageImpl<>(list, pageableOptional.orElse(null), count);
	}

	private List<T> findAll(Optional<Query> queryOptional, Optional<Sort> sortOptional, Optional<Pageable> pageableOptional) {
		// get the list of sorting with primitive field
		List<Order> orderPrimitiveList = sortOptional//
				.map(//
				mySortQuery -> StreamSupport.stream(mySortQuery.spliterator(), false)//
						.filter(order -> isTheFieldExistAndIsPrimitive(order.getProperty()))//
						.collect(Collectors.toList()) //
				)//
				.orElse(Collections.emptyList());
		// get the list of sorting with DBRef field
		List<Order> orderDBRefList = sortOptional//
				.map(//
				mySortQuery -> StreamSupport.stream(mySortQuery.spliterator(), false)//
						.filter(order -> isTheFieldExistAndIsDBRef(order.getProperty()))//
						.collect(Collectors.toList()))//
				.orElse(Collections.emptyList());
		Query query = queryOptional.orElse(new Query()).addCriteria(getFilterCriteria());
		if (orderDBRefList.isEmpty() && orderPrimitiveList.isEmpty()) {
			if (sortOptional.isPresent()) {
				query.with(sortOptional.get());
			}
			if (pageableOptional.isPresent()) {
				query.with(pageableOptional.get());
			}
			return mongoOperations.find(query, entityInformation.getJavaType(), entityInformation.getCollectionName());
		}
		Optional<Query> fitlerQuery = Optional.of(query);
		// FIXME sort: works for only one criteria
		// FIXME sort: for multi-criteria the sort is not specialized
		// (citeria 1, than 2 if entities both have same criteria 1)
		Set<T> result = new HashSet<>();
		if (!orderDBRefList.isEmpty()) {
			orderDBRefList.forEach(order -> result.addAll(findAllWithDBRef(fitlerQuery, order)));
		}
		if (!orderPrimitiveList.isEmpty()) {
			result.addAll(sortPrimitiveWithCaseInsensitive(fitlerQuery, orderPrimitiveList, pageableOptional));
		}
		return result.stream().collect(Collectors.toList());
	}

	private List<T> sortPrimitiveWithCaseInsensitive(Optional<Query> query, List<Order> orderPrimitiveList, Optional<Pageable> pageable) {
		DBCollection coll = mongoOperations.getCollection(entityInformation.getCollectionName());
		List<DBObject> pipe = new ArrayList<>();
		query.ifPresent(myQuery -> {
			DBObject match = new BasicDBObject();
			match.put("$match", myQuery.getQueryObject());
			pipe.add(match);
		});
		DBObject prjflds = new BasicDBObject();
		prjflds.put("doc", "$$ROOT");
		orderPrimitiveList.stream().forEach(order -> prjflds.put("insensitive" + order.getProperty(), new BasicDBObject("$toLower", "$" + order.getProperty())));
		DBObject project = new BasicDBObject();
		project.put("$project", prjflds);
		pipe.add(project);
		DBObject sortflds = new BasicDBObject();
		orderPrimitiveList.stream().forEach(order -> sortflds.put("insensitive" + order.getProperty(), Direction.ASC.equals(order.getDirection()) ? 1 : -1));
		DBObject sort = new BasicDBObject();
		sort.put("$sort", sortflds);
		pipe.add(sort);
		pageable.ifPresent(myPage -> {
			DBObject skip = new BasicDBObject();
			skip.put("$skip", myPage.getOffset());
			pipe.add(skip);
			DBObject limit = new BasicDBObject();
			limit.put("$limit", myPage.getPageSize());
			pipe.add(limit);
		});
		AggregationOutput agg = coll.aggregate(pipe);
		Stream<T> map = StreamSupport.stream(agg.results().spliterator(), true).map(result -> mongoOperations.getConverter().read(entityInformation.getJavaType(), (DBObject) result.get("doc")));
		return map.collect(Collectors.toList());
	}

	private List<T> findAllWithDBRef(Optional<Query> query, Order order) {
		Query queryFull = new Query();
		query.ifPresent(myQuery -> {
			for (String key : myQuery.getQueryObject().keySet()) {
				Object object = myQuery.getQueryObject().get(key);
				Criteria criteria = Criteria.where(key).is(object);
				queryFull.addCriteria(criteria);
			}
		});
		List<T> fullEntityList = mongoOperations.find(queryFull, entityInformation.getJavaType(), entityInformation.getCollectionName());
		int index = order.getProperty().indexOf(".");
		String dbRefFieldName = order.getProperty().substring(0, index);
		String fieldToSortInDBRef = order.getProperty().substring(index + 1);
		Field dbRefField = ReflectionUtils.findField(entityInformation.getJavaType(), dbRefFieldName);
		ReflectionUtils.makeAccessible(dbRefField);
		List<Object> entityDBRefObjectList = fullEntityList.parallelStream().map(entity -> {
			try {
				return dbRefField.get(entity);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
		}).filter(myObject -> myObject != null).collect(Collectors.toList());
		if (entityDBRefObjectList != null && entityDBRefObjectList.size() != 0) {
			Class<? extends Object> myClass = entityDBRefObjectList.get(0).getClass();
			List<String> entityDBRefIdList = entityDBRefObjectList.stream().map(object -> (DeleteEntityAbstract.class.cast(object)).getId()).collect(Collectors.toList());
			// TODO add the sort to this function
			List<? extends Object> dbRefList = mongoOperations.find(
					queryFull.addCriteria(Criteria.where(DeleteEntityAbstract.ID_FIELD_NAME).in(entityDBRefIdList)).with(new Sort(order.getDirection(), fieldToSortInDBRef)), myClass);
			Collections.sort(fullEntityList, new Comparator<T>() {
				@Override
				public int compare(T left, T right) {
					try {
						return Integer.compare(dbRefList.indexOf(dbRefField.get(left)), dbRefList.indexOf(dbRefField.get(right)));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						return 0;
					}
				}
			});
		}
		return fullEntityList;
	}

	private long count(Query query) {
		return mongoOperations.getCollection(entityInformation.getCollectionName()).count(query.getQueryObject());
	}

	protected static <T> List<T> convertIterableToList(Iterable<T> entities) {
		if (entities instanceof List) {
			return (List<T>) entities;
		}
		return StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());
	}

	private boolean isTheFieldExistAndIsPrimitive(String fieldName) {
		try {
			Field orderField = entityInformation.getJavaType().getDeclaredField(fieldName);
			return !ClassUtils.isPrimitiveOrWrapper(orderField.getType());
		} catch (NoSuchFieldException | SecurityException e) {
			// nothing to do
		}
		return false;
	}

	private boolean isTheFieldExistAndIsDBRef(String fieldName) {
		try {
			if (fieldName.indexOf(".") != -1) {
				Field orderField = entityInformation.getJavaType().getDeclaredField(fieldName.substring(0, fieldName.indexOf(".")));
				return orderField.getAnnotation(DBRef.class) != null;
			}
		} catch (NoSuchFieldException | SecurityException e) {
			// nothing to do
		}
		return false;
	}

	protected abstract Criteria getIdCriteria(Object id);

	protected abstract Criteria getFilterCriteria();
}
