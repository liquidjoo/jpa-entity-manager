package persistence.loader;

import persistence.metadata.EntityMetadata;
import query.ResultSetMapper;
import query.SelectQueryBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class SimpleEntityLoader {
    private final Connection connection;
    private final ResultSetMapper mapper;

    public SimpleEntityLoader(Connection connection, ResultSetMapper mapper) {
        this.connection = connection;
        this.mapper = mapper;
    }

    public <T> T load(Class<T> entityClass, Object id) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entityClass);

        String sql = new SelectQueryBuilder()
                .select("*")
                .from(metadata.getTableName())
                .where(metadata.getIdColumnName() + " = ?")
                .build();

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setObject(1, id);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return mapper.mapRow(rs, entityClass);
        }

        return null;
    }
}
