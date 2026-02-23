package persistence.context;

import entity.User;
import org.junit.jupiter.api.*;
import persistence.loader.SimpleEntityLoader;
import persistence.persister.SimpleEntityPersister;
import query.ResultSetMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActionQueueTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
        connection.setAutoCommit(false);

        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), age INT)"
        );

        connection.createStatement().execute("DELETE FROM users");
        connection.commit();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("persist(): INSERT를 지연시킨다")
    void testPersistDelaysInsert() throws Exception {
        SimplePersistenceContext context = new SimplePersistenceContext();

        User user = new User(null, "John", 25);
        assertNull(user.getId(), "persist 전에는 ID가 null이어야 함");

        context.persist(user);

        ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        assertEquals(0, rs.getInt(1), "flush 전에는 DB에 없어야 함");
    }

    @Test
    @Order(2)
    @DisplayName("persist() + flush(): INSERT가 실행된다")
    void testPersistAndFlush() throws Exception {
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = new User(null, "John", 25);
        context.persist(user);

        context.flush(connection, persister);
        connection.commit();

        ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        assertEquals(1, rs.getInt(1), "flush 후에는 DB에 있어야 함");

        assertNotNull(user.getId(), "flush 후에는 ID가 생성되어야 함");
        assertEquals(1L, user.getId(), "첫 번째 User의 ID는 1이어야 함");
    }

    @Test
    @Order(3)
    @DisplayName("persist(): 이미 영속화된 Entity는 persist 불가")
    void testPersistThrowsExceptionForManagedEntity() throws Exception {
        SimplePersistenceContext context = new SimplePersistenceContext();

        User user = new User(1L, "John", 25);  // ID가 이미 있음

        assertThrows(IllegalStateException.class, () -> {
            context.persist(user);
        }, "이미 영속화된 Entity는 persist 불가");
    }

    @Test
    @Order(4)
    @DisplayName("remove(): DELETE를 지연시킨다")
    void testRemoveDelaysDelete() throws Exception {
        connection.createStatement().execute("INSERT INTO users (id, name, age) VALUES (1, 'John', 25)");
        connection.commit();

        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        User user = context.find(User.class, 1L, loader);

        context.remove(user);

        ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users WHERE id = 1");
        rs.next();
        assertEquals(1, rs.getInt(1), "flush 전에는 DB에 있어야 함");
    }

    @Test
    @Order(5)
    @DisplayName("remove() + flush(): DELETE가 실행된다")
    void testRemoveAndFlush() throws Exception {
        connection.createStatement().execute("INSERT INTO users (id, name, age) VALUES (1, 'John', 25)");
        connection.commit();

        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = context.find(User.class, 1L, loader);
        context.remove(user);

        context.flush(connection, persister);
        connection.commit();

        ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users WHERE id = 1");
        rs.next();
        assertEquals(0, rs.getInt(1), "flush 후에는 DB에서 삭제되어야 함");
    }

    @Test
    @Order(6)
    @DisplayName("remove(): 영속 상태가 아닌 Entity는 remove 불가")
    void testRemoveThrowsExceptionForTransientEntity() throws Exception {
        SimplePersistenceContext context = new SimplePersistenceContext();
        User user = new User(1L, "John", 25);  // Transient 상태 (영속화 안됨)

        // When & Then: IllegalStateException 발생
        assertThrows(IllegalStateException.class, () -> {
            context.remove(user);
        }, "Transient 상태의 Entity는 remove 불가");
    }

    @Test
    @Order(7)
    @DisplayName("Write-Behind: INSERT → UPDATE → DELETE 순서로 실행")
    void testWriteBehindIntegration() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        // 1. INSERT
        User newUser = new User(null, "Alice", 20);
        context.persist(newUser);

        context.flush(connection, persister);
        connection.commit();

        assertNotNull(newUser.getId(), "INSERT 후 ID가 생성되어야 함");
        Long userId = newUser.getId();

        // 2. SELECT + UPDATE
        User user = context.find(User.class, userId, loader);
        user.setAge(21);

        context.flush(connection, persister);
        connection.commit();

        // DB 확인
        ResultSet rs = connection.createStatement().executeQuery(
                "SELECT age FROM users WHERE id = " + userId
        );
        rs.next();
        assertEquals(21, rs.getInt("age"), "UPDATE가 반영되어야 함");

        // 3. DELETE
        context.remove(user);

        context.flush(connection, persister);
        connection.commit();

        // DB 확인
        rs = connection.createStatement().executeQuery(
                "SELECT COUNT(*) FROM users WHERE id = " + userId
        );
        rs.next();
        assertEquals(0, rs.getInt(1), "DELETE가 반영되어야 함");
    }

    @Test
    @Order(8)
    @DisplayName("Write-Behind: 여러 Entity의 작업을 한 번에 처리")
    void testWriteBehindBatch() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user1 = new User(null, "John", 25);
        User user2 = new User(null, "Jane", 30);
        User user3 = new User(null, "Bob", 35);

        // When: 여러 Entity persist
        context.persist(user1);
        context.persist(user2);
        context.persist(user3);

        // 한 번의 flush로 모두 INSERT
        context.flush(connection, persister);
        connection.commit();

        // Then: 모두 DB에 저장됨
        ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        assertEquals(3, rs.getInt(1), "3개의 User가 모두 저장되어야 함");
    }

}