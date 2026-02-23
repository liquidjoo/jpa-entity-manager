package query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SelectQueryBuilder {

    private final List<String> columns;
    private String table;
    private String whereClause;
    private String orderByColumn;
    private String orderByDirection;
    private Integer limit;
    private Integer offset;
    private boolean built = false;

    public SelectQueryBuilder() {
        this.columns = new ArrayList<>();
    }


    public SelectQueryBuilder select(String... columns) {
        if (columns == null || columns.length == 0) {
            throw new IllegalArgumentException("Columns cannot be null or empty");
        }

        // 기존 컬럼에 추가 (여러 번 호출 가능)
        this.columns.addAll(Arrays.asList(columns));
        return this;
    }


    public SelectQueryBuilder from(String table) {
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        this.table = table.trim();
        return this;
    }


    public SelectQueryBuilder where(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("WHERE condition cannot be null or empty");
        }

        this.whereClause = condition.trim();
        return this;
    }

    public SelectQueryBuilder orderBy(String column, String direction) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Order by column cannot be null or empty");
        }

        if (direction == null || (!direction.equalsIgnoreCase("ASC") && !direction.equalsIgnoreCase("DESC"))) {
            throw new IllegalArgumentException("Direction must be 'ASC' or 'DESC'");
        }

        this.orderByColumn = column.trim();
        this.orderByDirection = direction.toUpperCase();
        return this;
    }


    public SelectQueryBuilder limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive, but was: " + limit);
        }

        this.limit = limit;
        return this;
    }

    public SelectQueryBuilder offset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative, but was: " + offset);
        }

        this.offset = offset;
        return this;
    }


    public String build() {
        // 재사용 방지 (선택적 구현)
        if (built) {
            throw new IllegalStateException("Builder has already been used. Create a new instance.");
        }

        // 필수 검증
        if (table == null) {
            throw new IllegalStateException("FROM clause is required");
        }

        // SQL 생성
        StringBuilder sql = new StringBuilder();

        // SELECT 절
        sql.append("SELECT ");
        if (columns.isEmpty()) {
            sql.append("*");  // 기본값
        } else {
            sql.append(String.join(", ", columns));
        }

        // FROM 절
        sql.append(" FROM ").append(table);

        // WHERE 절 (선택적)
        if (whereClause != null) {
            sql.append(" WHERE ").append(whereClause);
        }

        // ORDER BY 절 (선택적)
        if (orderByColumn != null) {
            sql.append(" ORDER BY ").append(orderByColumn).append(" ").append(orderByDirection);
        }

        // LIMIT 절 (선택적)
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        // OFFSET 절 (선택적, 도전 과제)
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }

        built = true;  // 재사용 방지
        return sql.toString();
    }

    public SelectQueryBuilder reset() {
        this.columns.clear();
        this.table = null;
        this.whereClause = null;
        this.orderByColumn = null;
        this.orderByDirection = null;
        this.limit = null;
        this.offset = null;
        this.built = false;
        return this;
    }
}
