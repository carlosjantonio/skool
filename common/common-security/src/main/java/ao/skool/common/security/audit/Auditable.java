package ao.skool.common.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose invocation should be recorded in the audit log.
 * The interceptor captures actor, tenant, action name, and (optionally) target IDs
 * returned by the method or supplied as arguments.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Short action name, e.g. "grade.update", "invoice.void". */
    String action();

    /** SpEL over method args, e.g. "#studentId". Optional. */
    String targetExpression() default "";
}
