package persistence.manager;


import persistence.context.SimplePersistenceContext;
import persistence.loader.SimpleEntityLoader;
import persistence.persister.SimpleEntityPersister;
import query.ResultSetMapper;

import java.sql.Connection;
import java.sql.SQLException;


public class SimpleEntityManager {
    private final Connection connection;

    private final SimplePersistenceContext persistenceContext;
    private final SimpleEntityLoader loader;
    private final SimpleEntityPersister persister;
    private final EntityTransaction transaction;

    // 상태 관리
    private boolean open = true;

    public SimpleEntityManager(Connection connection) {
        this.connection = connection;
        this.persistenceContext = new SimplePersistenceContext();
        this.loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        this.persister = new SimpleEntityPersister();
        this.transaction = new EntityTransaction(connection, persistenceContext, persister);
    }

    public EntityTransaction getTransaction() {
        checkOpen();
        return transaction;
    }

    public SimplePersistenceContext getPersistenceContext() {
        checkOpen();
        return persistenceContext;
    }

    public <T> T find(Class<T> entityClass, Object id) throws Exception {
        checkOpen();
        return persistenceContext.find(entityClass, id, loader);
    }


    public void persist(Object entity) throws Exception {
        checkOpen();
        checkTransaction();
        persistenceContext.persist(entity);
    }

    public void remove(Object entity) throws Exception {
        checkOpen();
        checkTransaction();
        persistenceContext.remove(entity);
    }

    public void flush() throws Exception {
        checkOpen();
        checkTransaction();
        persistenceContext.flush(connection, persister);
    }

    public void clear() {
        checkOpen();
        persistenceContext.clear();
        System.out.println("[Clear] 영속성 컨텍스트 초기화");
    }

    public void close() throws SQLException {
        if (!open) {
            return;
        }

        try {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            persistenceContext.clear();

            if (!connection.isClosed()) {
                connection.close();
            }

            open = false;
            System.out.println("[Close] EntityManager 종료");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close EntityManager", e);
        }
    }

    public boolean isOpen() {
        return open;
    }

    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("EntityManager가 이미 종료되었습니다");
        }
    }

    private void checkTransaction() {
        if (!transaction.isActive()) {
            throw new IllegalStateException(
                    "트랜잭션이 시작되지 않았습니다. getTransaction().begin()을 먼저 호출하세요"
            );
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
