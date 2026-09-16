package ao.skool.app.phase0;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the test harness itself.
 * <p>
 * Hibernate's {@code ddl-auto: create-drop} reports a failed {@code CREATE TABLE} as a
 * WARN and carries on, so a table can go missing from the test schema without anything
 * turning red until some unrelated test fails with "table not found". That is exactly
 * what happened to {@code grades}: H2 reserves {@code VALUE} as a keyword (Postgres does
 * not), so the column holding the 0–20 mark broke the statement and the whole table was
 * absent while the suite still reported success.
 * <p>
 * This test asserts every mapped entity is actually queryable. If a new entity introduces
 * another Postgres-vs-H2 keyword clash, this fails first and names the table.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaIntegrityTest {

    @Autowired private EntityManagerFactory emf;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("every mapped entity has a real table in the test schema")
    void everyEntityTableExists() {
        Map<String, String> broken = new TreeMap<>();
        List<String> checked = new ArrayList<>();

        for (EntityType<?> entity : emf.getMetamodel().getEntities()) {
            String table = tableNameOf(entity);
            checked.add(table);
            try {
                jdbc.queryForObject("select count(*) from " + table, Long.class);
            } catch (Exception e) {
                broken.put(table, rootMessage(e));
            }
        }

        assertThat(checked)
                .as("sanity: the metamodel should see every module's entities")
                .hasSizeGreaterThan(20);
        assertThat(broken)
                .as("tables missing from the H2 test schema — check for reserved-word clashes")
                .isEmpty();
    }

    private String tableNameOf(EntityType<?> entity) {
        Class<?> java = entity.getJavaType();
        Table table = java.getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            return table.name();
        }
        Entity annotation = java.getAnnotation(Entity.class);
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        // Hibernate's implicit strategy snake-cases the simple class name.
        return java.getSimpleName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        return msg == null ? root.toString() : msg.lines().findFirst().orElse(msg);
    }
}
