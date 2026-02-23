package query;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultSetMapperTest {

    public static class User {
        private Long id;
        private String name;
        private int age;

        public User() {
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    // snake_case 테스트용
    public static class UserProfile {
        private Long userId;
        private String userName;
        private Timestamp createdAt;

        public UserProfile() {
        }

        public Long getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public Timestamp getCreatedAt() {
            return createdAt;
        }
    }

    @Test
    void 단일_행_매핑() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnName(3)).thenReturn("age");

        when(rs.getLong(1)).thenReturn(1L);
        when(rs.getString(2)).thenReturn("John");
        when(rs.getInt(3)).thenReturn(30);

        ResultSetMapper mapper = new ResultSetMapper();
        User user = mapper.mapRow(rs, User.class);

        assertEquals(1L, user.getId());
        assertEquals("John", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void snake_case_변환() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnName(1)).thenReturn("user_id");
        when(metaData.getColumnName(2)).thenReturn("user_name");
        when(metaData.getColumnName(3)).thenReturn("created_at");

        Timestamp now = new Timestamp(System.currentTimeMillis());
        when(rs.getLong(1)).thenReturn(100L);
        when(rs.getString(2)).thenReturn("Alice");
        when(rs.getTimestamp(3)).thenReturn(now);

        ResultSetMapper mapper = new ResultSetMapper();
        UserProfile profile = mapper.mapRow(rs, UserProfile.class);

        assertEquals(100L, profile.getUserId());
        assertEquals("Alice", profile.getUserName());
        assertEquals(now, profile.getCreatedAt());
    }

    @Test
    void 여러_행_매핑() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnName(3)).thenReturn("age");

        when(rs.next()).thenReturn(true, true, true, false);

        when(rs.getLong(1)).thenReturn(1L, 2L, 3L);
        when(rs.getString(2)).thenReturn("John", "Alice", "Bob");
        when(rs.getInt(3)).thenReturn(30, 25, 35);

        ResultSetMapper mapper = new ResultSetMapper();
        List<User> users = mapper.mapRows(rs, User.class);

        assertEquals(3, users.size());

        assertEquals(1L, users.get(0).getId());
        assertEquals("John", users.get(0).getName());
        assertEquals(30, users.get(0).getAge());

        assertEquals(2L, users.get(1).getId());
        assertEquals("Alice", users.get(1).getName());
        assertEquals(25, users.get(1).getAge());

        assertEquals(3L, users.get(2).getId());
        assertEquals("Bob", users.get(2).getName());
        assertEquals(35, users.get(2).getAge());
    }

    @Test
    void NULL_값_처리() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(3);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnName(3)).thenReturn("age");

        when(rs.getLong(1)).thenReturn(1L);
        when(rs.getString(2)).thenReturn(null);  // NULL
        when(rs.getInt(3)).thenReturn(0);
        when(rs.wasNull()).thenReturn(false, false, true);  // age가 NULL

        ResultSetMapper mapper = new ResultSetMapper();
        User user = mapper.mapRow(rs, User.class);


        assertEquals(1L, user.getId());
        assertNull(user.getName());
    }

    @Test
    void 빈_ResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        when(rs.next()).thenReturn(false);  // 빈 ResultSet

        ResultSetMapper mapper = new ResultSetMapper();
        List<User> users = mapper.mapRows(rs, User.class);

        assertTrue(users.isEmpty());
    }

    @Test
    void 매칭되지_않는_컬럼은_무시() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(4);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");
        when(metaData.getColumnName(3)).thenReturn("age");
        when(metaData.getColumnName(4)).thenReturn("unknown_column");  // 매칭 안됨

        when(rs.getLong(1)).thenReturn(1L);
        when(rs.getString(2)).thenReturn("John");
        when(rs.getInt(3)).thenReturn(30);

        ResultSetMapper mapper = new ResultSetMapper();
        User user = mapper.mapRow(rs, User.class);

        assertEquals(1L, user.getId());
        assertEquals("John", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    void 기본_생성자_없는_클래스는_예외() {
        class NoDefaultConstructor {
            private final String name;

            public NoDefaultConstructor(String name) {
                this.name = name;
            }
        }

        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        try {
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(0);
        } catch (SQLException e) {
            fail("Mock setup failed");
        }

        ResultSetMapper mapper = new ResultSetMapper();

        assertThrows(RuntimeException.class, () -> {
            mapper.mapRow(rs, NoDefaultConstructor.class);
        });
    }
}
