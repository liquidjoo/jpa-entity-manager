package query;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class QueryExecutorTest {

    private static Connection connection;
    private QueryExecutor executor;

    public static class User {
        private Long id;
        private String name;
        private int age;

        public User() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @BeforeAll
    static void setupDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:queryExecutorTest;DB_CLOSE_DELAY=-1", "sa", "");

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE users (" +
                            "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "  name VARCHAR(100)," +
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
        executor = new QueryExecutor(connection);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
        }
    }

    @Test
    void selectOne_조회_성공() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
        }

        String sql = "SELECT * FROM users WHERE name = :name";
        User user = executor.selectOne(sql, Map.of("name", "John"), User.class);

        assertNotNull(user);
        assertEquals("John", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void selectOne_결과_없음() {
        String sql = "SELECT * FROM users WHERE name = :name";
        User user = executor.selectOne(sql, Map.of("name", "NonExistent"), User.class);

        assertNull(user);
    }

    @Test
    void selectList_여러_행_조회() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 35)");
        }

        String sql = "SELECT * FROM users WHERE age >= :minAge ORDER BY age";
        List<User> users = executor.selectList(sql, Map.of("minAge", 25), User.class);

        assertEquals(3, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("John", users.get(1).getName());
        assertEquals("Bob", users.get(2).getName());
    }

    @Test
    void selectList_빈_결과() {
        String sql = "SELECT * FROM users WHERE age > :age";
        List<User> users = executor.selectList(sql, Map.of("age", 100), User.class);

        assertTrue(users.isEmpty());
    }

    @Test
    void execute_INSERT() {
        String sql = "INSERT INTO users (name, age) VALUES (:name, :age)";
        int affected = executor.execute(sql, Map.of("name", "John", "age", 30));

        assertEquals(1, affected);
    }

    @Test
    void execute_UPDATE() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
        }

        String sql = "UPDATE users SET age = :age WHERE name = :name";
        int affected = executor.execute(sql, Map.of("name", "John", "age", 31));

        assertEquals(1, affected);

        String selectSql = "SELECT * FROM users WHERE name = :name";
        User user = executor.selectOne(selectSql, Map.of("name", "John"), User.class);
        assertEquals(31, user.getAge());
    }

    @Test
    void execute_DELETE() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
        }

        String sql = "DELETE FROM users WHERE name = :name";
        int affected = executor.execute(sql, Map.of("name", "John"));

        assertEquals(1, affected);

        String selectSql = "SELECT * FROM users WHERE name = :name";
        User user = executor.selectOne(selectSql, Map.of("name", "John"), User.class);
        assertNull(user);
    }

    @Test
    void executeAndGetKey_생성된_키_반환() {
        String sql = "INSERT INTO users (name, age) VALUES (:name, :age)";
        Long id = executor.executeAndGetKey(sql, Map.of("name", "John", "age", 30));

        assertNotNull(id);
        assertTrue(id > 0);

        String selectSql = "SELECT * FROM users WHERE id = :id";
        User user = executor.selectOne(selectSql, Map.of("id", id), User.class);
        assertEquals("John", user.getName());
    }

    @Test
    void 같은_파라미터_여러_번_사용() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 30)");
        }

        String sql = "SELECT * FROM users WHERE age = :age OR age = :age + 10";
        List<User> users = executor.selectList(sql, Map.of("age", 30), User.class);

        assertEquals(2, users.size());
    }

    @Test
    void 복잡한_WHERE_절() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (name, age) VALUES ('John', 30)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Alice', 25)");
            stmt.execute("INSERT INTO users (name, age) VALUES ('Bob', 35)");
        }

        String sql = "SELECT * FROM users " +
                "WHERE (age BETWEEN :minAge AND :maxAge) " +
                "AND name != :excludeName " +
                "ORDER BY age";

        List<User> users = executor.selectList(sql,
                Map.of("minAge", 25, "maxAge", 35, "excludeName", "Bob"),
                User.class
        );

        assertEquals(2, users.size());
        assertEquals("Alice", users.get(0).getName());
        assertEquals("John", users.get(1).getName());
    }

    @Test
    void NULL_Connection_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QueryExecutor(null)
        );
    }
}
