package query;

import java.util.LinkedHashMap;
import java.util.Map;


public class UpdateQueryBuilder {

    private String table;
    private final Map<String, String> setValues;
    private String whereClause;
    private boolean built = false;

    public UpdateQueryBuilder() {
        this.setValues = new LinkedHashMap<>();
    }

    public UpdateQueryBuilder table(String table) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        this.table = table.trim();
        return this;
    }


    public UpdateQueryBuilder set(String column, String value) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null. Use '?' for parameters.");
        }

        this.setValues.put(column.trim(), value.trim());
        return this;
    }


    public UpdateQueryBuilder values(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }

        values.forEach((column, value) -> {
            if (column == null || value == null) {
                throw new IllegalArgumentException("Column and value cannot be null");
            }
        });

        this.setValues.putAll(values);
        return this;
    }


    public UpdateQueryBuilder where(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE condition cannot be null or empty");
        }

        this.whereClause = condition.trim();
        return this;
    }


    public String build() {
        if (built) {
            throw new IllegalStateException("Builder has already been used");
        }

        // 검증
        if (table == null) {
            throw new IllegalStateException("Table name is required (use table() method)");
        }

        if (setValues.isEmpty()) {
            throw new IllegalStateException("At least one SET clause is required");
        }

        // WHERE 절 필수 검증 (안전성)
        if (whereClause == null) {
            throw new IllegalStateException(
                    "WHERE clause is required for UPDATE to prevent accidental mass updates. " +
                            "If you really want to update all rows, use where(\"1=1\")"
            );
        }

        // SQL 생성
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(table);

        sql.append(" SET ");
        String setClause = String.join(", ",
                setValues.entrySet().stream()
                        .map(entry -> entry.getKey() + " = " + entry.getValue())
                        .toArray(String[]::new)
        );
        sql.append(setClause);

        sql.append(" WHERE ").append(whereClause);

        built = true;
        return sql.toString();
    }


    public UpdateQueryBuilder reset() {
        this.table = null;
        this.setValues.clear();
        this.whereClause = null;
        this.built = false;
        return this;
    }
}
