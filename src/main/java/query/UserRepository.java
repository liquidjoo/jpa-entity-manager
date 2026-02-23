package query;


import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class UserRepository {

    private final QueryExecutor executor;

    public UserRepository(Connection connection) {
        this.executor = new QueryExecutor(connection);
    }

    public Optional<User> findById(Long id) {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "email", "age")
                .from("users")
                .where("id = :id")
                .build();

        User user = executor.selectOne(sql, Map.of("id", id), User.class);

        return Optional.ofNullable(user);
    }

    public List<User> findAll() {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "email", "age")
                .from("users")
                .orderBy("id", "ASC")
                .build();

        return executor.selectList(sql, Map.of(), User.class);
    }

    public List<User> findByNameLike(String namePattern) {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "email", "age")
                .from("users")
                .where("name LIKE :pattern")
                .orderBy("name", "ASC")
                .build();

        return executor.selectList(sql, Map.of("pattern", namePattern), User.class);
    }

    public List<User> findByAgeRange(int minAge, int maxAge) {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "email", "age")
                .from("users")
                .where("age BETWEEN :minAge AND :maxAge")
                .orderBy("age", "ASC")
                .build();

        return executor.selectList(sql, Map.of("minAge", minAge, "maxAge", maxAge), User.class);
    }

    public List<User> findByPage(int page, int size) {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "email", "age")
                .from("users")
                .orderBy("id", "ASC")
                .limit(size)
                .offset(page * size)
                .build();

        return executor.selectList(sql, Map.of(), User.class);
    }

    public Long insert(User user) {
        String sql = new InsertQueryBuilder()
                .into("users")
                .value("name", ":name")
                .value("email", ":email")
                .value("age", ":age")
                .build();

        return executor.executeAndGetKey(sql, Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "age", user.getAge()
        ));
    }

    public int update(User user) {
        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("name", ":name")
                .set("email", ":email")
                .set("age", ":age")
                .where("id = :id")
                .build();

        return executor.execute(sql, Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "age", user.getAge()
        ));
    }

    public int deleteById(Long id) {
        String sql = new DeleteQueryBuilder()
                .from("users")
                .where("id = :id")
                .build();

        return executor.execute(sql, Map.of("id", id));
    }

    public int deleteByAgeBelow(int maxAge) {
        String sql = new DeleteQueryBuilder()
                .from("users")
                .where("age < :maxAge")
                .build();

        return executor.execute(sql, Map.of("maxAge", maxAge));
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    public long count() {
        String sql = "SELECT COUNT(*) as cnt FROM users";

        CountResult result = executor.selectOne(sql, Map.of(), CountResult.class);
        return result != null ? result.getCnt() : 0;
    }

    public static class User {
        private Long id;
        private String name;
        private String email;
        private int age;

        public User() {
        }

        public User(Long id, String name, String email, int age) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.age = age;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class CountResult {
        private long cnt;

        public long getCnt() {
            return cnt;
        }

        public void setCnt(long cnt) {
            this.cnt = cnt;
        }

        public CountResult() {
        }
    }
}
