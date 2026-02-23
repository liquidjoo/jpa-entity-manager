package persistence.metadata;


import persistence.annotation.Column;
import persistence.annotation.Id;
import persistence.annotation.Table;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final Field idField;
    private final String idColumnName;
    private final List<Field> fields;
    private final Map<Field, String> columnNames;

    // 메타데이터 캐시: Class -> EntityMetadata
    private static final Map<Class<?>, EntityMetadata> CACHE = new ConcurrentHashMap<>();

    public static EntityMetadata of(Class<?> entityClass) {
        return CACHE.computeIfAbsent(entityClass, EntityMetadata::new);
    }

    private EntityMetadata(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.tableName = extractTableName(entityClass);
        this.idField = extractIdField(entityClass);
        this.idColumnName = extractColumnName(idField);
        this.fields = extractFields(entityClass);
        this.columnNames = extractColumnNames(fields);
    }

    private String extractTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            return entityClass.getAnnotation(Table.class).name();
        }
        return entityClass.getSimpleName().toLowerCase();
    }

    private Field extractIdField(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalArgumentException("@Id 필드가 없습니다: " + entityClass.getName());
    }

    private String extractColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).name();
        }
        return field.getName().toLowerCase();
    }

    private List<Field> extractFields(Class<?> entityClass) {
        List<Field> result = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            result.add(field);
        }
        return result;
    }

    private Map<Field, String> extractColumnNames(List<Field> fields) {
        Map<Field, String> result = new HashMap<>();
        for (Field field : fields) {
            result.put(field, extractColumnName(field));
        }
        return result;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public Field getIdField() {
        return idField;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getColumnName(Field field) {
        return columnNames.get(field);
    }

    public Map<Field, String> getColumnNames() {
        return columnNames;
    }
}
