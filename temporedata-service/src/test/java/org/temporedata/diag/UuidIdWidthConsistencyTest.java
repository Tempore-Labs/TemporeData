package org.temporedata.diag;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.GenericGenerators;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the recurring "Hibernate uuid2 (always 36 chars) mapped to a
 * VARCHAR(32) id column" regression that caused insert-time "Data too long" 500s
 * (fixed by V60/V61/V62). Deterministic, DB-free: for every @Entity whose @Id uses
 * the uuid2 generator, this test asserts that any declared @Column(length) on the id
 * is &gt;= 36. New migrations must keep a wide enough id column (or widen it).
 */
class UuidIdWidthConsistencyTest {

    private static final int UUID2_LENGTH = 36;

    @Test
    void uuid2IdColumnsMustBeWideEnough() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        List<String> violations = new ArrayList<>();
        int checked = 0;

        for (var candidate : scanner.findCandidateComponents("org.temporedata")) {
            Class<?> clazz = Class.forName(candidate.getBeanClassName());
            Map<String, String> generators = collectGenerators(clazz);
            Long declaredLength = uuid2IdColumnLength(clazz, generators);
            if (declaredLength == null) {
                continue; // not a uuid2 key (or no explicit column length)
            }
            checked++;
            if (declaredLength < UUID2_LENGTH) {
                violations.add(tableOf(clazz) + "." + idField(clazz).getName()
                        + " declared length=" + declaredLength
                        + " but uuid2 always produces " + UUID2_LENGTH + "-char ids (raises 500 on insert)");
            }
        }

        assertTrue(violations.isEmpty(),
                "uuid2 entities backed by id columns narrower than 36 chars (fix the DB migration or widen the column length):\n  "
                        + String.join("\n  ", violations));
        assertTrue(checked > 0, "expected to inspect at least one uuid2 @Entity; scanner found none");
    }

    /** Build name -> generator-strategy from class- and field-level @GenericGenerator(s). */
    private static Map<String, String> collectGenerators(Class<?> clazz) {
        Map<String, String> map = new HashMap<>();
        addGen(map, clazz.getAnnotation(GenericGenerator.class));
        GenericGenerators many = clazz.getAnnotation(GenericGenerators.class);
        if (many != null) {
            for (GenericGenerator g : many.value()) {
                addGen(map, g);
            }
        }
        for (Field f : clazz.getDeclaredFields()) {
            addGen(map, f.getAnnotation(GenericGenerator.class));
            GenericGenerators fmany = f.getAnnotation(GenericGenerators.class);
            if (fmany != null) {
                for (GenericGenerator g : fmany.value()) {
                    addGen(map, g);
                }
            }
        }
        return map;
    }

    private static void addGen(Map<String, String> map, GenericGenerator g) {
        if (g != null && g.strategy() != null) {
            map.put(g.name(), g.strategy());
        }
    }

    /**
     * @return the declared @Column length of a uuid2 @Id, or null when the entity has
     *         no uuid2 key / no explicit length (default Hibernate handling, no bug).
     */
    private static Long uuid2IdColumnLength(Class<?> clazz, Map<String, String> generators) throws Exception {
        Field id = idField(clazz);
        if (id == null) {
            return null;
        }
        GeneratedValue gv = id.getAnnotation(GeneratedValue.class);
        String genName = gv == null ? null : gv.generator();
        if (genName == null || !"uuid2".equalsIgnoreCase(generators.get(genName))) {
            return null;
        }
        Column col = id.getAnnotation(Column.class);
        return col == null ? null : (long) col.length();
    }

    private static Field idField(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return f;
            }
        }
        return null;
    }

    private static String tableOf(Class<?> clazz) {
        javax.persistence.Table t = clazz.getAnnotation(javax.persistence.Table.class);
        return t == null || t.name() == null || t.name().isBlank() ? clazz.getSimpleName() : t.name();
    }
}