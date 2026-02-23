package query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class SelectQueryBuilderTest {

    @Test
    void 기본_SELECT_쿼리_생성() {
        String sql = new SelectQueryBuilder()
                .select("id", "name")
                .from("users")
                .build();

        assertEquals("SELECT id, name FROM users", sql);
    }

    @Test
    void SELECT_없이_호출하면_기본값_사용() {
        String sql = new SelectQueryBuilder()
                .from("users")
                .build();

        assertEquals("SELECT * FROM users", sql);
    }

    @Test
    void FROM_없이_빌드하면_예외_발생() {
        SelectQueryBuilder builder = new SelectQueryBuilder()
                .select("*");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("FROM"));
    }

    @Test
    void WHERE_절_추가() {
        String sql = new SelectQueryBuilder()
                .select("*")
                .from("users")
                .where("age >= 20")
                .build();

        assertEquals("SELECT * FROM users WHERE age >= 20", sql);
    }

    @Test
    void ORDER_BY_추가() {
        String sql = new SelectQueryBuilder()
                .select("*")
                .from("users")
                .orderBy("age", "DESC")
                .build();

        assertEquals("SELECT * FROM users ORDER BY age DESC", sql);
    }

    @Test
    void ORDER_BY_방향_대소문자_무시() {
        String sql = new SelectQueryBuilder()
                .select("*")
                .from("users")
                .orderBy("age", "desc")  // 소문자
                .build();

        assertEquals("SELECT * FROM users ORDER BY age DESC", sql);
    }

    @Test
    void ORDER_BY_잘못된_방향() {
        SelectQueryBuilder builder = new SelectQueryBuilder()
                .from("users");

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.orderBy("age", "INVALID")
        );
    }

    @Test
    void LIMIT_추가() {
        String sql = new SelectQueryBuilder()
                .select("*")
                .from("users")
                .limit(5)
                .build();

        assertEquals("SELECT * FROM users LIMIT 5", sql);
    }

    @Test
    void LIMIT에_음수_전달하면_예외_발생() {
        SelectQueryBuilder builder = new SelectQueryBuilder();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> builder.limit(-1)
        );

        assertTrue(exception.getMessage().contains("positive"));
    }

    @Test
    void LIMIT에_0_전달하면_예외_발생() {
        SelectQueryBuilder builder = new SelectQueryBuilder();

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.limit(0)
        );
    }

    @Test
    void 모든_절_조합() {
        String sql = new SelectQueryBuilder()
                .select("id", "name", "age")
                .from("users")
                .where("age >= 20")
                .orderBy("name", "ASC")
                .limit(10)
                .build();

        assertEquals(
                "SELECT id, name, age FROM users WHERE age >= 20 ORDER BY name ASC LIMIT 10",
                sql
        );
    }

    @Test
    void 메서드_체이닝_작동() {
        // 메서드 체이닝이 잘 작동하는지 확인
        SelectQueryBuilder builder = new SelectQueryBuilder()
                .select("id")
                .select("name")  // 여러 번 호출 가능
                .from("users")
                .where("age > 20")
                .orderBy("id", "ASC")
                .limit(10);

        String sql = builder.build();

        assertTrue(sql.contains("id, name"));
    }

    @Test
    void NULL_컬럼_전달하면_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectQueryBuilder().select((String[]) null)
        );
    }

    @Test
    void 빈_컬럼_배열_전달하면_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectQueryBuilder().select()
        );
    }

    @Test
    void NULL_테이블_전달하면_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectQueryBuilder().from(null)
        );
    }

    @Test
    void 빈_테이블_이름_전달하면_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectQueryBuilder().from("  ")
        );
    }

    @Test
    void OFFSET_추가() {
        String sql = new SelectQueryBuilder()
                .select("*")
                .from("users")
                .limit(10)
                .offset(20)
                .build();

        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", sql);
    }

    @Test
    void OFFSET에_음수_전달하면_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SelectQueryBuilder().offset(-1)
        );
    }


    @Test
    void 빌더_재사용_시도하면_예외() {
        SelectQueryBuilder builder = new SelectQueryBuilder()
                .select("*")
                .from("users");

        builder.build();  // 첫 번째 build

        assertThrows(
                IllegalStateException.class,
                () -> builder.build()  // 두 번째 build - 예외!
        );
    }

    @Test
    void reset_후_재사용_가능() {
        SelectQueryBuilder builder = new SelectQueryBuilder()
                .select("*")
                .from("users");

        String sql1 = builder.build();
        assertEquals("SELECT * FROM users", sql1);

        builder.reset();

        String sql2 = builder.select("id")
                .from("products")
                .build();

        assertEquals("SELECT id FROM products", sql2);
    }

    @Test
    void 공백_처리() {
        String sql = new SelectQueryBuilder()
                .select("  id  ", "  name  ")
                .from("  users  ")
                .where("  age > 20  ")
                .orderBy("  name  ", "ASC")
                .build();

        // 공백이 정리되어야 함
        assertTrue(sql.contains("FROM users"));
        assertTrue(sql.contains("WHERE age > 20"));
        assertTrue(sql.contains("ORDER BY name"));
    }
}
