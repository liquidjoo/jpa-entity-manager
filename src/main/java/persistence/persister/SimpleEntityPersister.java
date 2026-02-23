package persistence.persister;


import persistence.annotation.Id;
import persistence.metadata.EntityMetadata;
import query.DeleteQueryBuilder;
import query.InsertQueryBuilder;
import query.UpdateQueryBuilder;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SimpleEntityPersister {

    public void insert(Object entity, Connection connection) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entity.getClass());

        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : metadata.getFields()) {
            if (field.isAnnotationPresent(Id.class)) continue;

            field.setAccessible(true);
            String columnName = metadata.getColumnName(field);
            Object value = field.get(entity);
            values.put(columnName, value);
        }

        InsertQueryBuilder builder = new InsertQueryBuilder()
                .into(metadata.getTableName());

        for (String column : values.keySet()) {
            builder.value(column, "?");
        }

        String sql = builder.build();

        PreparedStatement pstmt = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
        );

        int index = 1;
        for (Object value : values.values()) {
            pstmt.setObject(index++, value);
        }

        pstmt.executeUpdate();

        ResultSet generatedKeys = pstmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            Field idField = metadata.getIdField();
            idField.setAccessible(true);
            idField.set(entity, generatedKeys.getLong(1));
        }

        System.out.println("[INSERT] " + sql);
    }

    public void update(Object entity, int[] dirtyFields, Connection connection) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entity.getClass());
        List<Field> fields = metadata.getFields();

        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .table(metadata.getTableName());

        for (int index : dirtyFields) {
            Field field = fields.get(index);
            String columnName = metadata.getColumnName(field);
            builder.set(columnName, "?");
        }

        builder.where(metadata.getIdColumnName() + " = ?");

        String sql = builder.build();

        PreparedStatement pstmt = connection.prepareStatement(sql);

        int paramIndex = 1;
        for (int index : dirtyFields) {
            Field field = fields.get(index);
            field.setAccessible(true);
            pstmt.setObject(paramIndex++, field.get(entity));
        }

        Field idField = metadata.getIdField();
        idField.setAccessible(true);
        pstmt.setObject(paramIndex, idField.get(entity));

        pstmt.executeUpdate();

        System.out.println("[UPDATE] " + sql);
    }

    public void delete(Object entity, Connection connection) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entity.getClass());

        String sql = new DeleteQueryBuilder()
                .from(metadata.getTableName())
                .where(metadata.getIdColumnName() + " = ?")
                .build();

        PreparedStatement pstmt = connection.prepareStatement(sql);

        Field idField = metadata.getIdField();
        idField.setAccessible(true);
        pstmt.setObject(1, idField.get(entity));

        pstmt.executeUpdate();

        System.out.println("[DELETE] " + sql);
    }
}
