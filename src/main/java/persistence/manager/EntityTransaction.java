package persistence.manager;


import persistence.context.SimplePersistenceContext;
import persistence.persister.SimpleEntityPersister;

import java.sql.Connection;
import java.sql.SQLException;


public class EntityTransaction {
    private final Connection connection;
    private final SimplePersistenceContext persistenceContext;
    private final SimpleEntityPersister persister;
    private boolean active = false;

    public EntityTransaction(Connection connection,
                             SimplePersistenceContext persistenceContext,
                             SimpleEntityPersister persister) {
        this.connection = connection;
        this.persistenceContext = persistenceContext;
        this.persister = persister;
    }

    public void begin() {
        if (active) {
            throw new IllegalStateException("Transaction already active");
        }

        try {
            connection.setAutoCommit(false);
            active = true;
            System.out.println("[Transaction] 트랜잭션 시작");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    public void commit() {
        if (!active) {
            throw new IllegalStateException("Transaction not active");
        }

        try {
            persistenceContext.flush(connection, persister);

            connection.commit();
            connection.setAutoCommit(true);
            active = false;

            System.out.println("[Transaction] 커밋 완료");
        } catch (Exception e) {
            rollback();
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    public void rollback() {
        if (!active) {
            return;
        }

        try {
            connection.rollback();
            connection.setAutoCommit(true);
            persistenceContext.clear();  // 캐시 비우기
            active = false;

            System.out.println("[Transaction] 롤백 완료");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }

    public boolean isActive() {
        return active;
    }
}
