package persistence.loader;

import entity.User;
import org.junit.jupiter.api.*;
import persistence.context.EntityKey;
import persistence.context.SimplePersistenceContext;
import persistence.metadata.EntityMetadata;
import query.ResultSetMapper;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleEntityLoaderTest {

    private static Connection connection;

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
    @DisplayName("EntityKey: equals와 hashCode가 올바르게 동작한다")
    void testEntityKey() {
        // Given
        EntityKey key1 = new EntityKey(User.class, 1L);
        EntityKey key2 = new EntityKey(User.class, 1L);
        EntityKey key3 = new EntityKey(User.class, 2L);

        // Then
        assertEquals(key1, key2, "같은 타입과 ID를 가진 EntityKey는 같아야 함");
        assertNotEquals(key1, key3, "다른 ID를 가진 EntityKey는 달라야 함");
        assertEquals(key1.hashCode(), key2.hashCode(), "같은 EntityKey는 같은 hashCode를 가져야 함");
    }

    @Test
    @Order(2)
    @DisplayName("EntityMetadata: 테이블명과 ID 컬럼명을 올바르게 추출한다")
    void testEntityMetadata() {
        // When
        EntityMetadata metadata = EntityMetadata.of(User.class);

        // Then
        assertEquals("users", metadata.getTableName(), "테이블명이 'users'여야 함");
        assertEquals("id", metadata.getIdColumnName(), "ID 컬럼명이 'id'여야 함");
        assertNotNull(metadata.getIdField(), "ID 필드가 존재해야 함");
    }

    @Test
    @Order(3)
    @DisplayName("EntityMetadata: 같은 클래스는 캐싱된 메타데이터를 반환한다")
    void testEntityMetadataCaching() {
        // When
        EntityMetadata metadata1 = EntityMetadata.of(User.class);
        EntityMetadata metadata2 = EntityMetadata.of(User.class);

        // Then
        assertSame(metadata1, metadata2, "같은 클래스의 메타데이터는 캐싱되어야 함");
    }

    @Test
    @Order(4)
    @DisplayName("SimpleEntityLoader: 존재하는 Entity를 로드한다")
    void testEntityLoader() throws Exception {
        // Given
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        // When
        User user = loader.load(User.class, 1L);

        // Then
        assertNotNull(user, "User가 로드되어야 함");
        assertEquals(1L, user.getId(), "ID가 1L이어야 함");
        assertEquals("John", user.getName(), "이름이 'John'이어야 함");
        assertEquals(25, user.getAge(), "나이가 25여야 함");
    }

    @Test
    @Order(5)
    @DisplayName("SimpleEntityLoader: 존재하지 않는 ID는 null을 반환한다")
    void testEntityLoaderNotFound() throws Exception {
        // Given
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        // When
        User notFound = loader.load(User.class, 999L);

        // Then
        assertNull(notFound, "존재하지 않는 ID는 null을 반환해야 함");
    }

    @Test
    @Order(6)
    @DisplayName("1차 캐시: 같은 ID를 두 번 조회하면 같은 인스턴스를 반환한다")
    void testFirstLevelCache() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        // When
        User user1 = context.find(User.class, 1L, loader);
        User user2 = context.find(User.class, 1L, loader);

        // Then
        assertSame(user1, user2, "같은 ID는 같은 인스턴스를 반환해야 함 (1차 캐시)");
    }

    @Test
    @Order(7)
    @DisplayName("Identity 보장: 같은 ID의 Entity는 동일성을 보장한다")
    void testIdentity() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        // When
        User user1 = context.find(User.class, 1L, loader);
        User user2 = context.find(User.class, 1L, loader);

        // Then
        assertTrue(user1 == user2, "동일성(==) 보장: 같은 인스턴스여야 함");

        // 동일성 덕분에 데이터 일관성 보장
        user1.setAge(30);
        assertEquals(30, user2.getAge(), "user1 변경이 user2에 즉시 반영되어야 함");
    }

    @Test
    @Order(8)
    @DisplayName("1차 캐시: Cache Hit 시 DB 접근을 하지 않는다")
    void testCacheHit() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        // When: 첫 조회
        User user1 = context.find(User.class, 1L, loader);

        // DB 데이터 변경 (캐시와 불일치 상황 만들기)
        connection.createStatement().execute("UPDATE users SET name = 'Changed', age = 99 WHERE id = 1");
        connection.commit();

        // When: 두 번째 조회 (Cache Hit)
        User user2 = context.find(User.class, 1L, loader);

        // Then: 캐시에서 반환되므로 DB 변경사항이 반영되지 않음
        assertSame(user1, user2, "같은 인스턴스여야 함");
        assertEquals("John", user2.getName(), "캐시된 값이어야 함 (DB는 'Changed')");
        assertEquals(25, user2.getAge(), "캐시된 값이어야 함 (DB는 99)");
    }

    @Test
    @Order(9)
    @DisplayName("1차 캐시: clear() 후에는 캐시가 비워진다")
    void testCacheClear() throws Exception {
        // Given
        SimplePersistenceContext context = new SimplePersistenceContext();
        SimpleEntityLoader loader = new SimpleEntityLoader(connection, new ResultSetMapper());

        User user1 = context.find(User.class, 1L, loader);

        // When: 캐시 비우기
        context.clear();

        // DB 데이터 변경
        connection.createStatement().execute("UPDATE users SET name = 'Changed', age = 99 WHERE id = 1");
        connection.commit();

        // When: 다시 조회
        User user2 = context.find(User.class, 1L, loader);

        // Then: DB에서 새로 조회됨
        assertNotSame(user1, user2, "다른 인스턴스여야 함");
        assertEquals("Changed", user2.getName(), "DB에서 새로 조회한 값이어야 함");
        assertEquals(99, user2.getAge(), "DB에서 새로 조회한 값이어야 함");
    }
}