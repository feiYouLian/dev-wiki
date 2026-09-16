package com.sjbb.core.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SJCache<T> {

    private static final int defaultExpireTime = 3;// 3s

    private final Map<String, T> m_objects;
    private final Map<String, Long> m_expiredObjects;
    private final long m_lExpireTime;
    private final ExecutorService m_executor;

    public SJCache() {
        this(defaultExpireTime);
    }

    public SJCache(final int nExpireTime) {
        m_objects = Collections.synchronizedMap(new HashMap<>());
        m_expiredObjects = Collections.synchronizedMap(new HashMap<>());
        m_lExpireTime = nExpireTime;
        m_executor = Executors.newFixedThreadPool(256);
        Executors.newScheduledThreadPool(5).scheduleWithFixedDelay(RemoveExpiredObjects(), m_lExpireTime / 2,
                m_lExpireTime, TimeUnit.SECONDS);
    }

    private final Runnable RemoveExpiredObjects() {
        return () -> {
            for (final String name : m_expiredObjects.keySet()) {
                if (System.currentTimeMillis() > m_expiredObjects.get(name)) {
                    m_executor.execute(CreateRemoveRunnable(name));
                }
            }
        };
//        return new Runnable() {
//            public void run() {
//                for (final String name : m_expiredObjects.keySet()) {
//                    if (System.currentTimeMillis() > m_expiredObjects.get(name)) {
//                        m_executor.execute(CreateRemoveRunnable(name));
//                    }
//                }
//            }
//        };
    }

    private final Runnable CreateRemoveRunnable(final String name) {
        return () -> {
            m_objects.remove(name);
            m_expiredObjects.remove(name);
        };
//        return new Runnable() {
//            public void run() {
//                m_objects.remove(name);
//                m_expiredObjects.remove(name);
//            }
//        };
    }

    public long getExpireTime() {
        return m_lExpireTime;
    }

    public void put(final String name, final T obj) {
        put(name, obj, m_lExpireTime);
    }

    public void put(final String name, final T obj, final long expireTime) {
        m_objects.put(name, obj);
        m_expiredObjects.put(name, System.currentTimeMillis() + expireTime * 1000);
    }

    public T get(final String name) {
        final Long expireTime = m_expiredObjects.get(name);
        if (expireTime == null)
            return null;
        if (System.currentTimeMillis() > expireTime) {
            m_executor.execute(CreateRemoveRunnable(name));
            return null;
        }
        return m_objects.get(name);
    }

    public void remove(String name) {
        m_objects.remove(name);
        m_expiredObjects.remove(name);
    }
}

// public class SobotCache<T> {

//     private static final int defaultExpireTime = 3;// 3s

//     private final Map<String, T> m_objects;
//     private final Map<String, Long> m_expiredObjects;
//     private final long m_lExpireTime;
//     private final TimeUnit m_lExpireTimeUnit;
//     private final ExecutorService m_executor;

//     public SobotCache() {
//         this(defaultExpireTime, TimeUnit.SECONDS);
//     }

//     public SobotCache(final long nExpireTime, final TimeUnit unit) {
//         m_objects = new ConcurrentHashMap<>();
//         m_expiredObjects = new ConcurrentHashMap<>();
//         m_lExpireTime = nExpireTime;
//         m_lExpireTimeUnit = unit;
//         m_executor = Executors.newFixedThreadPool(128);
//         Executors.newScheduledThreadPool(5).scheduleWithFixedDelay(RemoveExpiredObjects(), m_lExpireTime / 2,
//                 m_lExpireTime, m_lExpireTimeUnit);
//     }

//     private final Runnable RemoveExpiredObjects() {
//         return () -> {
//             for (final String name : m_expiredObjects.keySet()) {
//                 if (System.currentTimeMillis() > m_expiredObjects.get(name)) {
//                     m_executor.execute(CreateRemoveRunnable(name));
//                 }
//             }
//         };
// //        return new Runnable() {
// //            public void run() {
// //                for (final String name : m_expiredObjects.keySet()) {
// //                    if (System.currentTimeMillis() > m_expiredObjects.get(name)) {
// //                        m_executor.execute(CreateRemoveRunnable(name));
// //                    }
// //                }
// //            }
// //        };
//     }

//     private final Runnable CreateRemoveRunnable(final String name) {
//         return () -> {
//             m_objects.remove(name);
//             m_expiredObjects.remove(name);
//         };
// //        return new Runnable() {
// //            public void run() {
// //                m_objects.remove(name);
// //                m_expiredObjects.remove(name);
// //            }
// //        };
//     }

//     public long getExpireTime() {
//         return m_lExpireTime;
//     }

//     public void put(final String name, final T obj) {
//         put(name, obj, m_lExpireTime);
//     }

//     public void put(final String name, final T obj, final long expireTime) {
//         m_objects.put(name, obj);
//         m_expiredObjects.put(name, System.currentTimeMillis() + m_lExpireTimeUnit.toMillis(expireTime));
//     }

//     public T get(final String name) {
//         final Long expireTime = m_expiredObjects.get(name);
//         if (expireTime == null)
//             return null;
//         if (System.currentTimeMillis() > expireTime) {
//             m_executor.execute(CreateRemoveRunnable(name));
//             return null;
//         }
//         return m_objects.get(name);
//     }

//     public void remove(String name) {
//         m_objects.remove(name);
//         m_expiredObjects.remove(name);
//     }

//     public static void main(String[] args) {
//         System.err.println(TimeUnit.SECONDS.toMillis(1));
//     }
// }
