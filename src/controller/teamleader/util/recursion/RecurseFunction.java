package controller.teamleader.util.recursion;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * A {@link java.util.function.BiFunction BiFunction} that can be used as a
 * target for recursion by for example
 * {@link Recursor#recurseUntil(RecurseFunction, Predicate, Object...)}.
 *
 * <p>
 * This is a <a href=
 * "https://docs.oracle.com/javase/8/docs/api/java/lang/FunctionalInterface.html">functional
 * interface</a> whose functional method is {@link #apply(Integer, Object[])}.
 *
 * @param <T>
 *            The element type of the <tt>list</tt> elements returned by the
 *            <tt>function</tt>.
 * @param <X>
 *            The type of the exception thrown.
 */
@FunctionalInterface
public interface RecurseFunction<T, X extends Throwable> {
	
	/**
	 * Applies the <tt>function</tt> to the given arguments.
	 *
	 * @param integer
	 *            The current iteration.
	 * @param objects
	 *            Optional arguments.
	 * @return A <tt>list</tt> containing elements of type
	 *         <tt>{@literal <T>}</tt>.
	 * @throws X
	 *             If something went wrong.
	 */
	ArrayList<T> apply(Integer integer, @SuppressWarnings("unused") Object[] objects) throws X;
	
}