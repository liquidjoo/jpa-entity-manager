package query;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class UpdateQueryBuilderTest {

    @Test
    void 기본_UPDATE_쿼리_생성() {
        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("name", "?")
                .set("age", "?")
                .where("id = ?")
                .build();

        assertEquals("UPDATE users SET name = ?, age = ? WHERE id = ?", sql);
    }

    @Test
    void 순서_보장_확인() {
        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("age", "?")
                .set("name", "?")
                .set("email", "?")
                .where("id = ?")
                .build();

        assertEquals("UPDATE users SET age = ?, name = ?, email = ? WHERE id = ?", sql);
    }

    @Test
    void Map으로_일괄_추가() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", "?");
        values.put("age", "?");
        values.put("email", "?");

        String sql = new UpdateQueryBuilder()
                .table("users")
                .values(values)
                .where("id = ?")
                .build();

        assertEquals("UPDATE users SET name = ?, age = ?, email = ? WHERE id = ?", sql);
    }

    @Test
    void 함수_사용_가능() {
        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("updated_at", "NOW()")
                .set("count", "count + 1")
                .set("name", "?")
                .where("id = ?")
                .build();

        assertEquals("UPDATE users SET updated_at = NOW(), count = count + 1, name = ? WHERE id = ?", sql);
    }

    @Test
    void WHERE_없이_빌드하면_예외() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .table("users")
                .set("name", "?");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("WHERE clause is required"));
        assertTrue(exception.getMessage().contains("prevent accidental mass updates"));
    }

    @Test
    void 전체_업데이트는_명시적으로_가능() {
        // 정말 전체를 업데이트하고 싶으면 where("1=1") 사용
        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("active", "false")
                .where("1=1")  // 명시적으로 전체 업데이트
                .build();

        assertEquals("UPDATE users SET active = false WHERE 1=1", sql);
    }

    @Test
    void TABLE_없이_빌드하면_예외() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .set("name", "?")
                .where("id = ?");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("Table name is required"));
    }

    @Test
    void SET_없이_빌드하면_예외() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .table("users")
                .where("id = ?");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("At least one SET"));
    }

    @Test
    void NULL_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().table(null)
        );
    }

    @Test
    void 빈_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().table("  ")
        );
    }

    @Test
    void NULL_컬럼명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().set(null, "?")
        );
    }

    @Test
    void NULL_값_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().set("name", null)
        );
    }

    @Test
    void NULL_WHERE_조건_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().where(null)
        );
    }

    @Test
    void 빈_WHERE_조건_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateQueryBuilder().where("  ")
        );
    }

    @Test
    void 빌더_재사용_방지() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .table("users")
                .set("name", "?")
                .where("id = ?");

        builder.build();  // 첫 번째 build

        assertThrows(
                IllegalStateException.class,
                () -> builder.build()  // 두 번째 build - 예외!
        );
    }

    @Test
    void reset_후_재사용_가능() {
        UpdateQueryBuilder builder = new UpdateQueryBuilder()
                .table("users")
                .set("name", "?")
                .where("id = ?");

        String sql1 = builder.build();
        assertEquals("UPDATE users SET name = ? WHERE id = ?", sql1);

        builder.reset();

        String sql2 = builder.table("products")
                .set("price", "?")
                .set("stock", "?")
                .where("id = ?")
                .build();

        assertEquals("UPDATE products SET price = ?, stock = ? WHERE id = ?", sql2);
    }

    @Test
    void 공백_처리() {
        String sql = new UpdateQueryBuilder()
                .table("  users  ")
                .set("  name  ", "  ?  ")
                .set("  age  ", "  ?  ")
                .where("  id = ?  ")
                .build();

        assertEquals("UPDATE users SET name = ?, age = ? WHERE id = ?", sql);
    }

    @Test
    void set과_values_혼합_사용() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", "?");
        values.put("age", "?");

        String sql = new UpdateQueryBuilder()
                .table("users")
                .set("id", "?")
                .values(values)
                .set("email", "?")
                .where("id = ?")
                .build();
        
        assertEquals("UPDATE users SET id = ?, name = ?, age = ?, email = ? WHERE id = ?", sql);
    }
}
