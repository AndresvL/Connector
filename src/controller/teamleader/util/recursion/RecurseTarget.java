package controller.teamleader.util.recursion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An <tt>annotation</tt> that indicates the the annotated method can be used as
 * a <a href=
 * "https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">method
 * reference</a> to a {@link RecurseFunction} parameter.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RecurseTarget {
}