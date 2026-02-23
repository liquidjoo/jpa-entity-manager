package query;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ResultSetMapper {

    public <T> T mapRow(ResultSet rs, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Map<String, Field> fieldMap = getFieldMap(clazz);

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String fieldName = snakeToCamel(columnName);
                Field field = fieldMap.get(fieldName.toLowerCase());

                if (field != null) {
                    field.setAccessible(true);

                    Object value = getValueByType(rs, i, field.getType());
                    field.set(instance, value);
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to map ResultSet to " + clazz.getName(), e);
        }
    }

    public <T> List<T> mapRows(ResultSet rs, Class<T> clazz) {
        try {
            List<T> results = new ArrayList<>();

            while (rs.next()) {
                results.add(mapRow(rs, clazz));
            }

            return results;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to map ResultSet rows to " + clazz.getName(), e);
        }
    }

    private Map<String, Field> getFieldMap(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            fieldMap.put(field.getName().toLowerCase(), field);
        }

        return fieldMap;
    }

    private String snakeToCamel(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    private Object getValueByType(ResultSet rs, int columnIndex, Class<?> fieldType) throws SQLException {
        if (fieldType == int.class || fieldType == Integer.class) {
            int value = rs.getInt(columnIndex);
            return rs.wasNull() ? null : value;
        }

        if (fieldType == long.class || fieldType == Long.class) {
            long value = rs.getLong(columnIndex);
            return rs.wasNull() ? null : value;
        }

        if (fieldType == String.class) {
            return rs.getString(columnIndex);
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            boolean value = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : value;
        }

        if (fieldType == double.class || fieldType == Double.class) {
            double value = rs.getDouble(columnIndex);
            return rs.wasNull() ? null : value;
        }

        if (fieldType == java.sql.Date.class) {
            return rs.getDate(columnIndex);
        }

        if (fieldType == java.sql.Timestamp.class) {
            return rs.getTimestamp(columnIndex);
        }

        throw new IllegalArgumentException("Unsupported field type: " + fieldType.getName());
    }
}
