package query;


import java.sql.*;
import java.util.List;
import java.util.Map;


public class QueryExecutor {

    private final Connection connection;
    private final ResultSetMapper mapper;


    public QueryExecutor(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        this.connection = connection;
        this.mapper = new ResultSetMapper();
    }

    public <T> T selectOne(String sql, Map<String, Object> params, Class<T> clazz) {
        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return mapper.mapRow(rs, clazz);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute selectOne: " + sql, e);
        }
    }

    public <T> List<T> selectList(String sql, Map<String, Object> params, Class<T> clazz) {
        try (PreparedStatement ps = prepareStatement(sql, params);
             ResultSet rs = ps.executeQuery()) {

            return mapper.mapRows(rs, clazz);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute selectList: " + sql, e);
        }
    }

    public int execute(String sql, Map<String, Object> params) {
        try (PreparedStatement ps = prepareStatement(sql, params)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute: " + sql, e);
        }
    }

    public Long executeAndGetKey(String sql, Map<String, Object> params) {
        NamedParameterQuery query = new NamedParameterQuery(sql);

        try (PreparedStatement ps = connection.prepareStatement(
                query.getParsedQuery(),
                Statement.RETURN_GENERATED_KEYS)) {

            // 파라미터 바인딩
            bindParameters(ps, query, params);

            ps.executeUpdate();

            // 생성된 키 조회
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get generated key");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to executeAndGetKey: " + sql, e);
        }
    }

    private PreparedStatement prepareStatement(String sql, Map<String, Object> params) throws SQLException {
        // 1. Named Parameter 변환
        NamedParameterQuery query = new NamedParameterQuery(sql);

        // 2. PreparedStatement 생성
        PreparedStatement ps = connection.prepareStatement(query.getParsedQuery());

        // 3. 파라미터 바인딩
        bindParameters(ps, query, params);

        return ps;
    }

    private void bindParameters(PreparedStatement ps, NamedParameterQuery query, Map<String, Object> params)
            throws SQLException {

        if (params == null || params.isEmpty()) {
            return;
        }

        // 각 파라미터에 대해
        for (String paramName : query.getParameterNames()) {
            Object value = params.get(paramName);

            // 같은 파라미터가 여러 번 나오는 경우 모두 바인딩
            List<Integer> indexes = query.getParameterIndexes(paramName);

            for (Integer index : indexes) {
                setParameter(ps, index, value);
            }
        }
    }

    private void setParameter(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
            return;
        }

        if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        } else if (value instanceof Double) {
            ps.setDouble(index, (Double) value);
        } else if (value instanceof Date) {
            ps.setDate(index, (Date) value);
        } else if (value instanceof Timestamp) {
            ps.setTimestamp(index, (Timestamp) value);
        } else {
            // 지원하지 않는 타입
            throw new IllegalArgumentException("Unsupported parameter type: " + value.getClass().getName());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
