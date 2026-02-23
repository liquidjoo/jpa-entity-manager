package persistence.context;

import persistence.persister.SimpleEntityPersister;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ActionQueue {
    private final List<EntityAction> insertions = new ArrayList<>();
    private final List<EntityAction> updates = new ArrayList<>();
    private final List<EntityAction> deletions = new ArrayList<>();

    public void addInsertAction(Object entity) {
        insertions.add(new InsertAction(entity));
    }

    public void addUpdateAction(Object entity, int[] dirtyFields) {
        updates.add(new UpdateAction(entity, dirtyFields));
    }

    public void addDeleteAction(Object entity) {
        deletions.add(new DeleteAction(entity));
    }

    public void executeActions(SimpleEntityPersister persister, Connection connection) throws Exception {
        for (EntityAction action : insertions) {
            action.execute(persister, connection);
        }

        for (EntityAction action : updates) {
            action.execute(persister, connection);
        }

        for (EntityAction action : deletions) {
            action.execute(persister, connection);
        }

        clear();
    }

    public void clear() {
        insertions.clear();
        updates.clear();
        deletions.clear();
    }

    interface EntityAction {
        void execute(SimpleEntityPersister persister, Connection connection) throws Exception;
    }

    static class InsertAction implements EntityAction {
        private final Object entity;

        public InsertAction(Object entity) {
            this.entity = entity;
        }

        @Override
        public void execute(SimpleEntityPersister persister, Connection connection) throws Exception {
            persister.insert(entity, connection);
        }
    }

    static class UpdateAction implements EntityAction {
        private final Object entity;
        private final int[] dirtyFields;

        public UpdateAction(Object entity, int[] dirtyFields) {
            this.entity = entity;
            this.dirtyFields = dirtyFields;
        }

        @Override
        public void execute(SimpleEntityPersister persister, Connection connection) throws Exception {
            persister.update(entity, dirtyFields, connection);
        }
    }

    static class DeleteAction implements EntityAction {
        private final Object entity;

        public DeleteAction(Object entity) {
            this.entity = entity;
        }

        @Override
        public void execute(SimpleEntityPersister persister, Connection connection) throws Exception {
            persister.delete(entity, connection);
        }
    }
}
