package controller.teamleader.util.recursion;

import org.apache.http.client.HttpResponseException;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * A Utility Class for <tt>recursion</tt>.
 */
public abstract class Recursor {
	
	/**
	 * Recurse over a given API-calling <tt>function</tt> until the given
	 * <tt>predicate</tt> doesn't hold.
	 *
	 * @param <T>
	 *            The type of the entities this method should return.
	 * @param function
	 *            A {@link RecurseFunction Function} to recurse over.
	 * @param predicate
	 *            A {@link Predicate}, which when applied indicates whether or
	 *            not to continue recursing.
	 * @param args
	 *            Optional arguments to pass to the <tt>function</tt>.
	 *
	 * @return A <tt>list</tt> of entities, accumulated from the returning
	 *         results of the <tt>function</tt>.
	 *
	 * @throws HttpResponseException
	 *             If something in the <tt>function</tt> went wrong.
	 */
	private static <T> ArrayList<T> recurseUntil(RecurseFunction<T, HttpResponseException> function,
			Predicate<ArrayList<T>> predicate, Object... args) throws HttpResponseException {
		ArrayList<T> list = new ArrayList<>();
		int i = 0;
		do {
			list.addAll(function.apply(i++, args));
		} while (predicate.test(list));
		return list;
	}
	
	/**
	 * A default implementation of the
	 * {@link Recursor#recurseUntil(RecurseFunction, Predicate, Object...)
	 * recurseUntil} method. This <tt>function</tt> will keep going until the
	 * last element in the <tt>list</tt> is not <code>null</code>.
	 */
	public static <T> ArrayList<T> recurseImpl(RecurseFunction<T, HttpResponseException> function, Object... args)
			throws HttpResponseException {
		return recurseUntil(function, arrayList -> arrayList.size() > 0 && arrayList.get(arrayList.size() - 1) == null,
				args);
	}
	
}