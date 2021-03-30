package pers.clare.core.lock;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 依 ID 建立鎖
 */
public abstract class IdLock<T> {
    // ID 鎖
    protected final Map<Object, T> locks = new HashMap<>();

    protected final Class<T> clazz;

    {
        Type type = this.getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            clazz = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
        } else {
            clazz = (Class<T>) Object.class;
        }
    }

    /**
     * get lock object by id
     *
     * @param id
     * @return
     */
    public T getLock(Object id, Object... args) {
        T lock = locks.get(id);
        if (lock != null) {
            return lock;
        }
        synchronized (locks) {
            lock = locks.get(id);
            if (lock != null) {
                return lock;
            }
            locks.put(id, lock = newInstance(args));
        }
        return lock;
    }

    public T remove(Object id) {
        synchronized (locks) {
            return locks.remove(id);
        }
    }

    protected T newInstance(Object... args) {
        try {
            if (args.length == 0) {
                return clazz.getDeclaredConstructor().newInstance();
            }
            Class<?>[] types = new Class[args.length];
            int i = 0;
            for (Object arg : args) types[i++] = arg.getClass();
            return clazz.getDeclaredConstructor(types).newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
