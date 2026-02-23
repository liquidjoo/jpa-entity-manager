package query;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import query.UserRepository.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


class UserRepositoryTest {

    private static Connection connection;
    private UserRepository repository;

    @BeforeAll
    static void setupDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:userRepositoryTest;DB_CLOSE_DELAY=-1", "sa", "");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE users (" +
                            "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "  name VARCHAR(100)," +
                            "  email VARCHAR(100)," +
                            "  age INT" +
                            ")"
            );
        }
    }

    @AfterAll
    static void teardownDatabase() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @BeforeEach
    void setup() throws SQLException {
        repository = new UserRepository(connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        }
    }

    @Test
    void insert_사용자_추가() {
        User user = new User(null, "John", "john@example.com", 30);

        Long id = repository.insert(user);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    void findById_조회_성공() {
        User user = new User(null, "John", "john@example.com", 30);
        Long id = repository.insert(user);

        Optional<User> found = repository.findById(id);

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
        assertEquals("john@example.com", found.get().getEmail());
        assertEquals(30, found.get().getAge());
    }

    @Test
    void findById_존재하지_않음() {
        Optional<User> found = repository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void findAll_전체_조회() {
        repository.insert(new User(null, "John", "john@example.com", 30));
        repository.insert(new User(null, "Alice", "alice@example.com", 25));
        repository.insert(new User(null, "Bob", "bob@example.com", 35));

        List<User> users = repository.findAll();

        assertEquals(3, users.size());
        assertEquals("John", users.get(0).getName());
        assertEquals("Alice", users.get(1).getName());
        assertEquals("Bob", users.get(2).getName());
    }

    @Test
    void update_사용자_수정() {
        Long id = repository.insert(new User(null, "John", "john@example.com", 30));

        User user = new User(id, "John Updated", "john.updated@example.com", 31);
        int affected = repository.update(user);

        assertEquals(1, affected);

        Optional<User> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("John Updated", found.get().getName());
        assertEquals("john.updated@example.com", found.get().getEmail());
        assertEquals(31, found.get().getAge());
    }

    @Test
    void deleteById_사용자_삭제() {
        Long id = repository.insert(new User(null, "John", "john@example.com", 30));

        int affected = repository.deleteById(id);

        assertEquals(1, affected);

        Optional<User> found = repository.findById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void findByNameLike_이름_검색() {
        repository.insert(new User(null, "John", "john@example.com", 30));
        repository.insert(new User(null, "Johnny", "johnny@example.com", 25));
        repository.insert(new User(null, "Alice", "alice@example.com", 35));

        List<User> users = repository.findByNameLike("John%");

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> u.getName().startsWith("John")));
    }

    @Test
    void findByAgeRange_나이_범위_검색() {
        repository.insert(new User(null, "John", "john@example.com", 30));
        repository.insert(new User(null, "Alice", "alice@example.com", 25));
        repository.insert(new User(null, "Bob", "bob@example.com", 35));

        List<User> users = repository.findByAgeRange(25, 32);

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());  // age 오름차순
        assertEquals("John", users.get(1).getName());
    }

    @Test
    void findByPage_페이징_조회() {
        for (int i = 1; i <= 10; i++) {
            repository.insert(new User(null, "User" + i, "user" + i + "@example.com", 20 + i));
        }

        List<User> page0 = repository.findByPage(0, 3);

        assertEquals(3, page0.size());
        assertEquals("User1", page0.get(0).getName());
        assertEquals("User2", page0.get(1).getName());
        assertEquals("User3", page0.get(2).getName());

        List<User> page1 = repository.findByPage(1, 3);

        assertEquals(3, page1.size());
        assertEquals("User4", page1.get(0).getName());
        assertEquals("User5", page1.get(1).getName());
        assertEquals("User6", page1.get(2).getName());
    }

    @Test
    void deleteByAgeBelow_나이_기준_삭제() {
        repository.insert(new User(null, "John", "john@example.com", 30));
        repository.insert(new User(null, "Alice", "alice@example.com", 25));
        repository.insert(new User(null, "Bob", "bob@example.com", 35));

        int affected = repository.deleteByAgeBelow(30);

        assertEquals(1, affected);  // Alice만 삭제

        List<User> remaining = repository.findAll();
        assertEquals(2, remaining.size());
        assertTrue(remaining.stream().allMatch(u -> u.getAge() >= 30));
    }

    @Test
    void existsById_존재_확인() {
        Long id = repository.insert(new User(null, "John", "john@example.com", 30));

        assertTrue(repository.existsById(id));
        assertFalse(repository.existsById(999L));
    }

    @Test
    void count_전체_개수() {
        repository.insert(new User(null, "John", "john@example.com", 30));
        repository.insert(new User(null, "Alice", "alice@example.com", 25));
        repository.insert(new User(null, "Bob", "bob@example.com", 35));

        long count = repository.count();

        assertEquals(3, count);
    }

    @Test
    void count_빈_테이블() {
        long count = repository.count();

        assertEquals(0, count);
    }

    @Test
    void CRUD_전체_플로우() {
        User user = new User(null, "John", "john@example.com", 30);
        Long id = repository.insert(user);
        assertNotNull(id);

        Optional<User> found = repository.findById(id);
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());

        User updated = new User(id, "John Updated", "john.updated@example.com", 31);
        int affected = repository.update(updated);
        assertEquals(1, affected);

        Optional<User> afterUpdate = repository.findById(id);
        assertEquals("John Updated", afterUpdate.get().getName());

        int deleted = repository.deleteById(id);
        assertEquals(1, deleted);

        Optional<User> afterDelete = repository.findById(id);
        assertFalse(afterDelete.isPresent());
    }
}
