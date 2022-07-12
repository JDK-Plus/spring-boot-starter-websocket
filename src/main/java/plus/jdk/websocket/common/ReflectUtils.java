package plus.jdk.websocket.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectUtils {

    public static List<Method> getAnnotatedMethods(Object obj, Class<? extends Annotation> clazz, IFilter<Method> iFilter) {
        List<Method> results = new ArrayList<>();
        Method[] methods = obj.getClass().getDeclaredMethods();
        for(Method method : methods) {
            if(method.getAnnotation(clazz) != null && iFilter.valid(method)) {
                results.add(method);
            }
        }
        return results;
    }

    public static Method getFirstAnnotatedMethod(Object obj, Class<? extends Annotation> clazz, IFilter<Method> iFilter) {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for(Method method : methods) {
            method.setAccessible(true);
            if(method.getAnnotation(clazz) != null && iFilter.valid(method)) {
                return method;
            }
        }
        return null;
    }
}
