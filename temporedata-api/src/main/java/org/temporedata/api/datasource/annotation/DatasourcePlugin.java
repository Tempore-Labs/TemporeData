package org.temporedata.api.datasource.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a {@code DatasourceProcessor} as a built-in or uploaded datasource
 * plugin, mirroring DolphinScheduler's plugin registration metadata.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DatasourcePlugin {

    /** Uppercase type key, e.g. MYSQL. */
    String value();

    /** Display name, e.g. MySQL. */
    String name() default "";

    /** Whether this plugin ships with the platform (true) or was uploaded. */
    boolean builtin() default true;
}