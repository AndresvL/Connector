package controller.teamleader.util;

import java.lang.reflect.Method;

public abstract class BeanComparator {
	
	public static <T> boolean areFieldsEqual(T o1, T o2) {
		try {
			final Method[] methods = o1.getClass().getDeclaredMethods();
			
			boolean eq = true;
			for (Method method : methods) {
				// Not a getter Method
				if (!method.getName().startsWith("get"))
					continue;
				
				Object m1 = method.invoke(o1), m2 = method.invoke(o2);
				
				eq = eq && m1 == null ? m2 == null : m1.equals(m2);
			}
			return eq;
		} catch (Throwable ignore) {
			return false;
		}
	}
	
}