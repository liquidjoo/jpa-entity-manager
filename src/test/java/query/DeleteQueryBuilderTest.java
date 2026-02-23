package query;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class DeleteQueryBuilderTest {

    @Test
    void 기본_DELETE_쿼리_생성() {
        String sql = new DeleteQueryBuilder()
                .from("users")
                .where("id = ?")
                .build();

        assertEquals("DELETE FROM users WHERE id = ?", sql);
    }

    @Test
    void 복잡한_WHERE_조건() {
        String sql = new DeleteQueryBuilder()
                .from("users")
                .where("age < 20 AND status = 'inactive'")
                .build();

        assertEquals("DELETE FROM users WHERE age < 20 AND status = 'inactive'", sql);
    }

    @Test
    void WHERE_없이_빌드하면_예외() {
        DeleteQueryBuilder builder = new DeleteQueryBuilder()
                .from("users");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("WHERE clause is required"));
        assertTrue(exception.getMessage().contains("prevent accidental mass deletions"));
    }


    @Test
    void FROM_없이_빌드하면_예외() {
        DeleteQueryBuilder builder = new DeleteQueryBuilder()
                .where("id = ?");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("Table name is required"));
    }

    @Test
    void NULL_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteQueryBuilder().from(null)
        );
    }

    @Test
    void 빈_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteQueryBuilder().from("  ")
        );
    }

    @Test
    void NULL_WHERE_조건_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteQueryBuilder().where(null)
        );
    }

    @Test
    void 빈_WHERE_조건_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteQueryBuilder().where("  ")
        );
    }

    @Test
    void 빌더_재사용_방지() {
        DeleteQueryBuilder builder = new DeleteQueryBuilder()
                .from("users")
                .where("id = ?");

        builder.build();

        assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );
    }

    @Test
    void reset_후_재사용_가능() {
        DeleteQueryBuilder builder = new DeleteQueryBuilder()
                .from("users")
                .where("id = ?");

        String sql1 = builder.build();
        assertEquals("DELETE FROM users WHERE id = ?", sql1);

        builder.reset();

        String sql2 = builder.from("products")
                .where("stock = 0")
                .build();

        assertEquals("DELETE FROM products WHERE stock = 0", sql2);
    }

    @Test
    void 공백_처리() {
        String sql = new DeleteQueryBuilder()
                .from("  users  ")
                .where("  id = ?  ")
                .build();

        assertEquals("DELETE FROM users WHERE id = ?", sql);
    }

    @Test
    void IN_절_사용() {
        String sql = new DeleteQueryBuilder()
                .from("users")
                .where("id IN (?, ?, ?)")
                .build();

        assertEquals("DELETE FROM users WHERE id IN (?, ?, ?)", sql);
    }

    @Test
    void BETWEEN_절_사용() {
        String sql = new DeleteQueryBuilder()
                .from("logs")
                .where("created_at BETWEEN ? AND ?")
                .build();

        assertEquals("DELETE FROM logs WHERE created_at BETWEEN ? AND ?", sql);
    }
}
