package persistence.context;


import persistence.loader.SimpleEntityLoader;
import persistence.metadata.EntityMetadata;
import persistence.persister.SimpleEntityPersister;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;


public class SimplePersistenceContext {
    private final Map<EntityKey, Object> entities = new HashMap<>();
    private final Map<Object, EntityEntry> entries = new HashMap<>();
    private final ActionQueue actionQueue = new ActionQueue();

    public <T> T find(Class<T> entityClass, Object id, SimpleEntityLoader loader) throws Exception {
        EntityKey key = new EntityKey(entityClass, id);

        Object cached = entities.get(key);
        if (cached != null) {
            System.out.println("[Cache Hit] " + key);
            return (T) cached;
        }

        System.out.println("[Cache Miss] Loading from DB...");
        T entity = loader.load(entityClass, id);

        if (entity != null) {
            entities.put(key, entity);

            // 스냅샷 저장 (Dirty Checking을 위해)
            Object[] snapshot = extractState(entity);
            EntityEntry entry = new EntityEntry(entity, snapshot);
            entries.put(entity, entry);
        }

        return entity;
    }

    public void persist(Object entity) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entity.getClass());
        Field idField = metadata.getIdField();
        idField.setAccessible(true);
        Object id = idField.get(entity);

        if (id != null) {
            throw new IllegalStateException("이미 영속화된 Entity입니다: " + entity);
        }

        actionQueue.addInsertAction(entity);

        System.out.println("[Persist] Entity 영속화 예약: " + entity.getClass().getSimpleName());
    }

    public void remove(Object entity) throws Exception {
        if (!entries.containsKey(entity)) {
            throw new IllegalStateException("영속 상태가 아닌 Entity입니다: " + entity);
        }

        actionQueue.addDeleteAction(entity);

        EntityEntry entry = entries.get(entity);
        entry.setStatus(EntityEntry.EntityStatus.DELETED);

        System.out.println("[Remove] Entity 삭제 예약: " + entity.getClass().getSimpleName());
    }

    public void flush(Connection connection, SimpleEntityPersister persister) throws Exception {
        for (Map.Entry<Object, EntityEntry> entry : entries.entrySet()) {
            Object entity = entry.getKey();
            EntityEntry entityEntry = entry.getValue();

            if (entityEntry.getStatus() == EntityEntry.EntityStatus.DELETED) {
                continue;
            }

            Object[] currentState = extractState(entity);

            if (entityEntry.isDirty(currentState)) {
                int[] dirtyFields = entityEntry.findModified(currentState);

                actionQueue.addUpdateAction(entity, dirtyFields);

                System.out.println("[Dirty Check] 변경 감지: " + entity.getClass().getSimpleName());
            }
        }

        actionQueue.executeActions(persister, connection);

        for (Map.Entry<Object, EntityEntry> entry : entries.entrySet()) {
            Object entity = entry.getKey();
            EntityEntry entityEntry = entry.getValue();

            if (entityEntry.getStatus() == EntityEntry.EntityStatus.MANAGED) {
                Object[] currentState = extractState(entity);
                entityEntry.updateSnapshot(currentState);
            }
        }

        System.out.println("[Flush] 모든 SQL 실행 완료");
    }

    public void clear() {
        entities.clear();
        entries.clear();
        actionQueue.clear();
    }

    private Object[] extractState(Object entity) throws Exception {
        EntityMetadata metadata = EntityMetadata.of(entity.getClass());
        java.util.List<Field> fields = metadata.getFields();

        Object[] state = new Object[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            field.setAccessible(true);
            state[i] = field.get(entity);
        }

        return state;
    }
}
