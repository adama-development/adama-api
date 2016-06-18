package com.adama.api.repository.util.repository.abst;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
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
	 * Creates a new {@link AdamaMongoRepositoryAbstract} for the given {@link MongoEntityInformation} and {@link MongoTemplate}.
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

	public abstract T findOne(ID id);

	public boolean exists(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return mongoOperations.exists(getIdQuery(id), entityInformation.getJavaType(), entityInformation.getCollectionName());
	}

	public long count() {
		return count(new Query());
	}

	public void delete(ID id) {

		Assert.notNull(id, "The given id must not be null!");
		T entity = findOne(id);
		Assert.notNull(entity, "Cannot find the entity with this id!");
		entity.setActive(false);
		save(entity);
	}

	public void delete(T entity) {

		Assert.notNull(entity, "The given entity must not be null!");
		delete(entityInformation.getId(entity));
	}

	public void delete(Iterable<? extends T> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(entity -> delete(entity));
	}

	public void deleteAll() {
		mongoOperations.findAll(entityInformation.getJavaType()).parallelStream().forEach(entity -> delete(entity));
	}

	public List<T> findAll() {
		return findAll(Optional.of(new Query()));
	}

	public Iterable<T> findAll(Iterable<ID> ids) {
		Set<ID> parameters = new HashSet<ID>(tryDetermineRealSizeOrReturn(ids, 10));
		ids.forEach(id -> parameters.add(id));
		return findAll(Optional.of(new Query(new Criteria(entityInformation.getIdAttribute()).in(parameters))));
	}

	public Page<T> findAll(final Pageable pageable) {

		Query query = new Query();
		List<T> list = findAll(Optional.ofNullable(query.with(pageable)), Optional.ofNullable(pageable.getSort()), Optional.ofNullable(pageable));
		Query queryCount = new Query(getFilterCriteria());
		Long count = count(queryCount);
		return new PageImpl<T>(list, pageable, count);
	}

	public List<T> findAll(Sort sort) {
		return findAll(Optional.ofNullable(new Query().with(sort)), Optional.ofNullable(sort), Optional.empty());
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

		Assert.notNull(entities, "The given Iterable of entities not be null!");

		List<S> list = convertIterableToList(entities);

		if (list.isEmpty()) {
			return list;
		}

		Stream<S> stream = list.parallelStream().peek(entity -> entity.setActive(true));

		mongoOperations.insertAll(stream.collect(Collectors.toList()));
		return list;
	}

	@Override
	public Page<T> search(String key, final Pageable pageable) {

		Query query = new Query();
		Field[] allFields = entityInformation.getJavaType().getDeclaredFields();
		List<Criteria> criterias = new ArrayList<Criteria>();
		Arrays.asList(allFields).parallelStream()
				.filter(field -> !ClassUtils.isPrimitiveOrWrapper(field.getType()) && Modifier.isPrivate(field.getModifiers()))
				.forEach(field -> criterias.add(Criteria.where(field.getName()).regex(key, "i")));
		List<T> list = findAll(Optional.ofNullable(query.addCriteria(
				new Criteria().orOperator((Criteria[]) criterias.toArray(new Criteria[criterias.size()]))).with(pageable)),
				Optional.ofNullable(pageable.getSort()), Optional.ofNullable(pageable));
		Long count = count(query);
		return new PageImpl<T>(list, pageable, count);
	}

	@Override
	public Page<T> findAllQueryPageable(Optional<Query> query, Pageable pageable) {
		List<T> list = findAll(query, Optional.empty(), Optional.ofNullable(pageable));
		Query countQuery = new Query();
		if (query.isPresent()) {
			countQuery = query.get();
		} else {
			countQuery.addCriteria(getFilterCriteria());
		}
		Long count = count(countQuery);
		return new PageImpl<T>(list, pageable, count);
	}

	@Override
	public List<T> findAll(Optional<Query> query) {
		if(query.isPresent()){
			return mongoOperations.find(query.get(), entityInformation.getJavaType(), entityInformation.getCollectionName());
		} else {
			return findAll();
		}						
	}

	@Override
	public T findOne(Optional<Query> query) {
		return query.map(myquery -> mongoOperations.findOne(myquery, entityInformation.getJavaType(), entityInformation.getCollectionName())).orElse(
				null);
	}

	private List<T> findAll(Optional<Query> query, Optional<Sort> sortQuery, Optional<Pageable> pageable) {

		// get the list of sorting with primitive field
		List<Order> orderPrimitiveList = sortQuery.map(
				mySortQuery -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(mySortQuery.iterator(), Spliterator.ORDERED), true)
						.filter(order -> isTheFieldExistAndIsPrimitive(order.getProperty())).collect(Collectors.toList())).orElse(
				Collections.emptyList());
		// get the list of sorting with DBRef field
		List<Order> orderDBRefList = sortQuery.map(
				mySortQuery -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(mySortQuery.iterator(), Spliterator.ORDERED), true)
						.filter(order -> isTheFieldExistAndIsDBRef(order.getProperty())).collect(Collectors.toList()))
				.orElse(Collections.emptyList());

		Optional<Query> fitlerQuery = Optional.ofNullable(query.map(filterQuery -> filterQuery.addCriteria(getFilterCriteria())).orElse(
				new Query(getFilterCriteria())));

		if (orderPrimitiveList.isEmpty()) {
			if (orderDBRefList.isEmpty()) {
				return findAll(fitlerQuery);
			} else {
				SortedSet<T> result = new TreeSet<T>();
				orderDBRefList.forEach(order -> result.addAll(findAllWithDBRef(fitlerQuery, order)));
				return result.stream().collect(Collectors.toList());
			}
		} else {
			if (orderDBRefList.isEmpty()) {
				return sortPrimitiveWithCaseInsensitive(fitlerQuery, orderPrimitiveList, pageable);
			} else {
				Set<T> result = new HashSet<>();
				sortQuery.ifPresent(mySortQuery -> StreamSupport.stream(
						Spliterators.spliteratorUnknownSize(mySortQuery.iterator(), Spliterator.ORDERED), true).forEach(order -> {
					if (orderDBRefList.contains(order)) {
						result.addAll(findAllWithDBRef(fitlerQuery, order));
					} else {
						result.addAll(sortPrimitiveWithCaseInsensitive(fitlerQuery, orderPrimitiveList, pageable));
					}
				}));

				return result.stream().collect(Collectors.toList());
			}
		}
	}

	private List<T> sortPrimitiveWithCaseInsensitive(Optional<Query> query, List<Order> orderPrimitiveList, Optional<Pageable> pageable) {
		DBCollection coll = mongoOperations.getCollection(entityInformation.getCollectionName());
		List<DBObject> pipe = new ArrayList<DBObject>();

		query.ifPresent(myQuery -> {
			DBObject match = new BasicDBObject();
			match.put("$match", myQuery.getQueryObject());
			pipe.add(match);
		});

		DBObject prjflds = new BasicDBObject();
		prjflds.put("doc", "$$ROOT");
		orderPrimitiveList.stream().forEach(
				order -> prjflds.put("insensitive" + order.getProperty(), new BasicDBObject("$toLower", "$" + order.getProperty())));

		DBObject project = new BasicDBObject();
		project.put("$project", prjflds);
		pipe.add(project);

		DBObject sortflds = new BasicDBObject();
		orderPrimitiveList.stream().forEach(
				order -> sortflds.put("insensitive" + order.getProperty(), Direction.ASC.equals(order.getDirection()) ? 1 : -1));

		DBObject sort = new BasicDBObject();
		sort.put("$sort", sortflds);
		pipe.add(sort);

		pageable.ifPresent(myPage -> {
			DBObject limit = new BasicDBObject();
			limit.put("$limit", myPage.getPageSize());
			pipe.add(limit);
			DBObject skip = new BasicDBObject();
			skip.put("$skip", myPage.getOffset());
			pipe.add(skip);
		});

		AggregationOutput agg = coll.aggregate(pipe);
		Stream<T> map = StreamSupport.stream(agg.results().spliterator(), true).map(
				result -> mongoOperations.getConverter().read(entityInformation.getJavaType(), (DBObject) result.get("doc")));

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
				Object deleteEntityAbstract = dbRefField.get(entity);
				if (deleteEntityAbstract != null) {
					return deleteEntityAbstract;
				} else {
					return null;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
		}).filter(myObject -> myObject != null).collect(Collectors.toList());
		if (entityDBRefObjectList != null && entityDBRefObjectList.size() != 0) {
			Class<? extends Object> myClass = entityDBRefObjectList.get(0).getClass();
			List<String> entityDBRefIdList = entityDBRefObjectList.stream().map(object -> (DeleteEntityAbstract.class.cast(object)).getId())
					.collect(Collectors.toList());
			// TODO add the sort to this function
			List<? extends Object> dbRefList = mongoOperations.find(
					queryFull.addCriteria(Criteria.where(DeleteEntityAbstract.ID_FIELD_NAME).in(entityDBRefIdList)).with(
							new Sort(order.getDirection(), fieldToSortInDBRef)), myClass);
			Collections.sort(fullEntityList, new Comparator<T>() {
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

		int capacity = tryDetermineRealSizeOrReturn(entities, 10);

		if (capacity == 0 || entities == null) {
			return Collections.<T> emptyList();
		}

		List<T> list = new ArrayList<T>(capacity);
		entities.forEach(entity -> list.add(entity));

		return list;
	}

	private static int tryDetermineRealSizeOrReturn(Iterable<?> iterable, int defaultSize) {
		return iterable == null ? 0 : (iterable instanceof Collection) ? ((Collection<?>) iterable).size() : defaultSize;
	}

	private boolean isTheFieldExistAndIsPrimitive(String fieldName) {
		try {
			Field orderField = entityInformation.getJavaType().getDeclaredField(fieldName);
			if (!ClassUtils.isPrimitiveOrWrapper(orderField.getType())) {
				return true;
			}
		} catch (NoSuchFieldException | SecurityException e) {
			return false;
		}
		return false;
	}

	private boolean isTheFieldExistAndIsDBRef(String fieldName) {
		try {
			if (fieldName.indexOf(".") != -1) {
				Field orderField = entityInformation.getJavaType().getDeclaredField(fieldName.substring(0, fieldName.indexOf(".")));
				if (orderField.getAnnotation(DBRef.class) != null) {
					return true;
				}
			} else {
				return false;
			}

		} catch (NoSuchFieldException | SecurityException e) {
			return false;
		}
		return false;
	}

	protected Query getIdQuery(Object id) {
		return new Query(getIdCriteria(id));
	}

	protected abstract Criteria getIdCriteria(Object id);

	protected abstract Criteria getFilterCriteria();

}
