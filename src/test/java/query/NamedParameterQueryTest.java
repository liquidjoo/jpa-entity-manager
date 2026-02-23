package query;


import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NamedParameterQueryTest {

    @Test
    void 단일_파라미터_변환() {
        String sql = "SELECT * FROM users WHERE id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE id = ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("id"));
        assertEquals(1, query.getParameterIndex(":id"));
    }

    @Test
    void 여러_파라미터_변환() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :name AND age > :age";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE id = ? AND name = ? AND age > ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("id"));
        assertEquals(2, query.getParameterIndex("name"));
        assertEquals(3, query.getParameterIndex("age"));
    }

    @Test
    void 같은_파라미터_여러_번_사용() {
        String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE id = ? OR parent_id = ?", query.getParsedQuery());

        assertEquals(1, query.getParameterIndex("id"));

        List<Integer> indexes = query.getParameterIndexes("id");
        assertEquals(2, indexes.size());
        assertEquals(1, indexes.get(0));
        assertEquals(2, indexes.get(1));
    }

    @Test
    void 파라미터_개수_확인() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :name";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals(2, query.getParameterCount());
        assertEquals(2, query.getTotalPlaceholderCount());
    }

    @Test
    void 중복_파라미터의_개수() {
        String sql = "SELECT * FROM users WHERE id = :id OR parent_id = :id OR manager_id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals(1, query.getParameterCount());
        assertEquals(3, query.getTotalPlaceholderCount());
    }

    @Test
    void 파라미터_이름_조회() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :name AND age > :age";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        Set<String> paramNames = query.getParameterNames();

        assertEquals(3, paramNames.size());
        assertTrue(paramNames.contains("id"));
        assertTrue(paramNames.contains("name"));
        assertTrue(paramNames.contains("age"));
    }

    @Test
    void 파라미터_존재_확인() {
        String sql = "SELECT * FROM users WHERE id = :id AND name = :name";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertTrue(query.hasParameter("id"));
        assertTrue(query.hasParameter(":id"));
        assertTrue(query.hasParameter("name"));
        assertFalse(query.hasParameter("age"));
        assertFalse(query.hasParameter("unknown"));
    }

    @Test
    void 존재하지_않는_파라미터_조회시_예외() {
        String sql = "SELECT * FROM users WHERE id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertThrows(
                IllegalArgumentException.class,
                () -> query.getParameterIndex("name")
        );
    }

    @Test
    void INSERT_쿼리() {
        String sql = "INSERT INTO users (id, name, age) VALUES (:id, :name, :age)";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("INSERT INTO users (id, name, age) VALUES (?, ?, ?)", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("id"));
        assertEquals(2, query.getParameterIndex("name"));
        assertEquals(3, query.getParameterIndex("age"));
    }

    @Test
    void UPDATE_쿼리() {
        String sql = "UPDATE users SET name = :name, age = :age WHERE id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("UPDATE users SET name = ?, age = ? WHERE id = ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("name"));
        assertEquals(2, query.getParameterIndex("age"));
        assertEquals(3, query.getParameterIndex("id"));
    }

    @Test
    void DELETE_쿼리() {
        String sql = "DELETE FROM users WHERE id = :id";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("DELETE FROM users WHERE id = ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("id"));
    }

    @Test
    void 언더스코어_포함_파라미터() {
        String sql = "SELECT * FROM users WHERE user_id = :user_id AND created_at > :created_at";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE user_id = ? AND created_at > ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("user_id"));
        assertEquals(2, query.getParameterIndex("created_at"));
    }

    @Test
    void 파라미터_없는_쿼리() {
        String sql = "SELECT * FROM users";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users", query.getParsedQuery());
        assertEquals(0, query.getParameterCount());
        assertEquals(0, query.getTotalPlaceholderCount());
    }

    @Test
    void NULL_SQL_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamedParameterQuery(null)
        );
    }

    @Test
    void 빈_SQL_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NamedParameterQuery("  ")
        );
    }

    @Test
    void IN_절_사용() {
        String sql = "SELECT * FROM users WHERE id IN (:id1, :id2, :id3)";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ?)", query.getParsedQuery());
        assertEquals(3, query.getParameterCount());
    }

    @Test
    void LIKE_절_사용() {
        String sql = "SELECT * FROM users WHERE name LIKE :pattern";
        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals("SELECT * FROM users WHERE name LIKE ?", query.getParsedQuery());
        assertEquals(1, query.getParameterIndex("pattern"));
    }

    @Test
    void 원본_쿼리_조회() {
        String originalSql = "SELECT * FROM users WHERE id = :id AND name = :name";
        NamedParameterQuery query = new NamedParameterQuery(originalSql);

        assertEquals(originalSql, query.getOriginalQuery());

        assertEquals("SELECT * FROM users WHERE id = ? AND name = ?", query.getParsedQuery());
    }

    @Test
    void 복잡한_WHERE_절() {
        String sql = "SELECT * FROM users " +
                "WHERE (name = :name OR email = :email) " +
                "AND age BETWEEN :minAge AND :maxAge " +
                "AND status = :status";

        NamedParameterQuery query = new NamedParameterQuery(sql);

        assertEquals(5, query.getParameterCount());
        assertEquals(1, query.getParameterIndex("name"));
        assertEquals(2, query.getParameterIndex("email"));
        assertEquals(3, query.getParameterIndex("minAge"));
        assertEquals(4, query.getParameterIndex("maxAge"));
        assertEquals(5, query.getParameterIndex("status"));
    }
}
