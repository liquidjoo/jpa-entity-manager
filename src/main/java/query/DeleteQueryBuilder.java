package query;


public class DeleteQueryBuilder {

    private String table;
    private String whereClause;
    private boolean built = false;


    public DeleteQueryBuilder from(String table) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        this.table = table.trim();
        return this;
    }


    public DeleteQueryBuilder where(String condition) {
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
            throw new IllegalStateException("Table name is required (use from() method)");
        }

        // WHERE 절 필수 검증 (안전성)
        if (whereClause == null) {
            throw new IllegalStateException(
                    "WHERE clause is required for DELETE to prevent accidental mass deletions. " +
                            "If you really want to delete all rows, use where(\"1=1\")"
            );
        }

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(table);
        sql.append(" WHERE ").append(whereClause);

        built = true;
        return sql.toString();
    }


    public DeleteQueryBuilder reset() {
        this.table = null;
        this.whereClause = null;
        this.built = false;
        return this;
    }
}
