package query;


import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class InsertQueryBuilderTest {

    @Test
    void 기본_INSERT_쿼리_생성() {
        String sql = new InsertQueryBuilder()
                .into("users")
                .value("id", "?")
                .value("name", "?")
                .value("age", "?")
                .build();

        assertEquals("INSERT INTO users (id, name, age) VALUES (?, ?, ?)", sql);
    }

    @Test
    void 순서_보장_확인() {
        String sql = new InsertQueryBuilder()
                .into("users")
                .value("age", "?")
                .value("name", "?")
                .value("id", "?")
                .build();

        assertEquals("INSERT INTO users (age, name, id) VALUES (?, ?, ?)", sql);
    }

    @Test
    void Map으로_일괄_추가() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("id", "?");
        values.put("name", "?");
        values.put("email", "?");

        String sql = new InsertQueryBuilder()
                .into("users")
                .values(values)
                .build();

        assertEquals("INSERT INTO users (id, name, email) VALUES (?, ?, ?)", sql);
    }

    @Test
    void 함수_사용_가능() {
        String sql = new InsertQueryBuilder()
                .into("users")
                .value("id", "UUID()")
                .value("created_at", "NOW()")
                .value("name", "?")
                .build();

        assertEquals("INSERT INTO users (id, created_at, name) VALUES (UUID(), NOW(), ?)", sql);
    }

    @Test
    void INTO_없이_빌드하면_예외() {
        InsertQueryBuilder builder = new InsertQueryBuilder()
                .value("name", "?");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("Table name is required"));
    }

    @Test
    void VALUE_없이_빌드하면_예외() {
        InsertQueryBuilder builder = new InsertQueryBuilder()
                .into("users");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> builder.build()
        );

        assertTrue(exception.getMessage().contains("At least one column-value"));
    }

    @Test
    void NULL_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().into(null)
        );
    }

    @Test
    void 빈_테이블명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().into("  ")
        );
    }

    @Test
    void NULL_컬럼명_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().value(null, "?")
        );
    }

    @Test
    void NULL_값_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().value("name", null)
        );
    }

    @Test
    void NULL_Map_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().values(null)
        );
    }

    @Test
    void 빈_Map_예외() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertQueryBuilder().values(new LinkedHashMap<>())
        );
    }

    @Test
    void 빌더_재사용_방지() {
        InsertQueryBuilder builder = new InsertQueryBuilder()
                .into("users")
                .value("name", "?");

        builder.build();  // 첫 번째 build

        assertThrows(
                IllegalStateException.class,
                () -> builder.build()  // 두 번째 build - 예외!
        );
    }

    @Test
    void reset_후_재사용_가능() {
        InsertQueryBuilder builder = new InsertQueryBuilder()
                .into("users")
                .value("name", "?");

        String sql1 = builder.build();
        assertEquals("INSERT INTO users (name) VALUES (?)", sql1);

        builder.reset();

        String sql2 = builder.into("products")
                .value("title", "?")
                .value("price", "?")
                .build();

        assertEquals("INSERT INTO products (title, price) VALUES (?, ?)", sql2);
    }

    @Test
    void 공백_처리() {
        String sql = new InsertQueryBuilder()
                .into("  users  ")
                .value("  name  ", "  ?  ")
                .value("  age  ", "  ?  ")
                .build();

        assertEquals("INSERT INTO users (name, age) VALUES (?, ?)", sql);
    }

    @Test
    void value와_values_혼합_사용() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", "?");
        values.put("age", "?");

        String sql = new InsertQueryBuilder()
                .into("users")
                .value("id", "?")
                .values(values)
                .value("email", "?")
                .build();

        assertEquals("INSERT INTO users (id, name, age, email) VALUES (?, ?, ?, ?)", sql);
    }
}
