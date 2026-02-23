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
class EntityEntryTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
        connection.setAutoCommit(false);

        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), age INT)"
        );

        connection.createStatement().execute("DELETE FROM users");

        connection.createStatement().execute(
                "INSERT INTO users (id, name, age) VALUES (1, 'John', 25)"
        );
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
    @DisplayName("EntityEntry: 변경되지 않은 상태는 Dirty하지 않다")
    void testEntityEntryNotDirty() {
        // Given
        User user = new User(1L, "John", 25);
        Object[] loadedState = new Object[]{1L, "John", 25};
        EntityEntry entry = new EntityEntry(user, loadedState);

        // When & Then
        assertFalse(entry.isDirty(new Object[]{1L, "John", 25}), "변경되지 않았으면 Dirty하지 않아야 함");
    }

    @Test
    @Order(2)
    @DisplayName("EntityEntry: 변경된 상태는 Dirty하다")
    void testEntityEntryIsDirty() {
        // Given
        User user = new User(1L, "John", 25);
        Object[] loadedState = new Object[]{1L, "John", 25};
        EntityEntry entry = new EntityEntry(user, loadedState);

        // When & Then
        assertTrue(entry.isDirty(new Object[]{1L, "John", 28}), "변경되었으면 Dirty해야 함");
    }

    @Test
    @Order(3)
    @DisplayName("EntityEntry: 변경된 필드의 인덱스를 찾을 수 있다")
    void testEntityEntryFindModified() {
        // Given
        User user = new User(1L, "John", 25);
        Object[] loadedState = new Object[]{1L, "John", 25};
        EntityEntry entry = new EntityEntry(user, loadedState);

        // When
        int[] modified = entry.findModified(new Object[]{1L, "John", 28});

        // Then
        assertEquals(1, modified.length, "1개 필드가 변경되었어야 함");
        assertEquals(2, modified[0], "age 필드(인덱스 2)가 변경되었어야 함");
    }

    @Test
    @Order(4)
    @DisplayName("EntityEntry: 스냅샷은 불변이다 (원본 배열 변경 시에도)")
    void testEntityEntrySnapshotImmutable() {
        // Given
        User user = new User(1L, "John", 25);
        Object[] loadedState = new Object[]{1L, "John", 25};
        EntityEntry entry = new EntityEntry(user, loadedState);

        // When: 원본 배열 변경
        loadedState[2] = 99;

        // Then: 스냅샷은 변경되지 않음
        assertFalse(entry.isDirty(new Object[]{1L, "John", 25}), "스냅샷은 불변이어야 함");
    }

    @Test
    @Order(5)
    @DisplayName("Dirty Checking: Entity 변경 시 자동 UPDATE")
    void testDirtyChecking() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = context.find(User.class, 1L, loader);

        // When: age 변경
        user.setAge(28);

        // flush 호출 (Dirty Checking 수행)
        context.flush(connection, persister);
        connection.commit();

        // Then: DB에 UPDATE 실행됨
        ResultSet rs = connection.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        rs.next();
        assertEquals(28, rs.getInt("age"), "DB에 UPDATE가 반영되어야 함");
    }

    @Test
    @Order(6)
    @DisplayName("Dirty Checking: 여러 필드 변경 감지")
    void testMultipleFieldChanges() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = context.find(User.class, 1L, loader);

        // When: name과 age 변경
        user.setName("Jane");
        user.setAge(30);

        context.flush(connection, persister);
        connection.commit();

        // Then: 모든 변경사항이 DB에 반영됨
        ResultSet rs = connection.createStatement().executeQuery("SELECT name, age FROM users WHERE id = 1");
        rs.next();
        assertEquals("Jane", rs.getString("name"), "name이 변경되어야 함");
        assertEquals(30, rs.getInt("age"), "age가 변경되어야 함");
    }

    @Test
    @Order(7)
    @DisplayName("Dirty Checking: 변경 없으면 UPDATE 실행 안함")
    void testNoUpdateWhenNotDirty() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = context.find(User.class, 1L, loader);
        // 변경 없음

        // When: flush (변경사항 없음)
        context.flush(connection, persister);
        connection.commit();

        // Then: DB 값은 그대로
        ResultSet rs = connection.createStatement().executeQuery("SELECT name, age FROM users WHERE id = 1");
        rs.next();
        assertEquals("John", rs.getString("name"), "원래 값이어야 함");
        assertEquals(25, rs.getInt("age"), "원래 값이어야 함");
    }

    @Test
    @Order(8)
    @DisplayName("Dirty Checking: 여러 번 변경해도 마지막 값만 반영")
    void testMultipleChangesLastValueApplied() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());
        SimpleEntityPersister persister = new SimpleEntityPersister();

        User user = context.find(User.class, 1L, loader);

        // When: 여러 번 변경
        user.setAge(26);
        user.setAge(27);
        user.setAge(28);  // 마지막 값

        context.flush(connection, persister);
        connection.commit();

        // Then: DB에는 마지막 값만 반영
        ResultSet rs = connection.createStatement().executeQuery("SELECT age FROM users WHERE id = 1");
        rs.next();
        assertEquals(28, rs.getInt("age"), "마지막 값이 반영되어야 함");
    }

}