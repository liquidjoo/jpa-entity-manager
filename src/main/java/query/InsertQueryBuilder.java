package query;

import java.util.LinkedHashMap;
import java.util.Map;


public class InsertQueryBuilder {

    private String table;
    private final Map<String, String> columnValues;
    private boolean built = false;

    public InsertQueryBuilder() {
        this.columnValues = new LinkedHashMap<>();
    }


    public InsertQueryBuilder into(String table) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        this.table = table.trim();
        return this;
    }


    public InsertQueryBuilder value(String column, String value) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null. Use '?' for parameters.");
        }

        this.columnValues.put(column.trim(), value.trim());
        return this;
    }


    public InsertQueryBuilder values(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        // putAll은 순서를 유지함 (LinkedHashMap의 경우)
        values.forEach((column, value) -> {
            if (column == null || value == null) {
                throw new IllegalArgumentException("Column and value cannot be null");
            }
        });

        this.columnValues.putAll(values);
        return this;
    }


    public String build() {
        if (built) {
            throw new IllegalStateException("Builder has already been used");
        }

        // 검증
        if (table == null) {
            throw new IllegalStateException("Table name is required (use into() method)");
        }

        if (columnValues.isEmpty()) {
            throw new IllegalStateException("At least one column-value pair is required");
        }

        // SQL 생성

        String sql = "INSERT INTO " + table +

                // 컬럼 부분: (col1, col2, col3)
                " (" +
                String.join(", ", columnValues.keySet()) +
                ")" +

                // 값 부분: VALUES (?, ?, ?)
                " VALUES (" +
                String.join(", ", columnValues.values()) +
                ")";

        built = true;
        return sql;
    }

    public InsertQueryBuilder reset() {
        this.table = null;
        this.columnValues.clear();
        this.built = false;
        return this;
    }
}
