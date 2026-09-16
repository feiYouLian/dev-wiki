package com.sjbb.core.token.service;


import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RedisService<T> {

    RedisTemplate<String, T> getRedisTemplate();

    /**
     * * 指定缓存失效时间
     *
     * @param key
     * @param time
     * @return
     */
    default boolean expire(String key, long time) {
        try {
            if (time > 0) {
                getRedisTemplate().expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * * 根据key 获取过期时间
     *
     * @param key
     * @return
     */
    default long getExpire(String key) {
        return getRedisTemplate().getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    default boolean hasKey(String key) {
        try {
            return getRedisTemplate().hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * * 判断缓存是否过期
     *
     * @param key
     * @return
     */
    default boolean isExpired(String key) {
        boolean isExpired = false;
        long expiredValue = getRedisTemplate().getExpire(key);
        if (expiredValue < 0)
            isExpired = true;
        return isExpired;
    }

    /**
     * * 删除缓存
     *
     * @param key
     */
    default void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                getRedisTemplate().delete(key[0]);
            } else {
                getRedisTemplate().delete(Arrays.asList(key));
//				getRedisTemplate().delete(CollectionUtils.arrayToList(key));
            }
        }
    }

    /**
     * * 获取
     *
     * @param key
     * @return
     */
    default T get(String key) {
        return key == null ? null : getRedisTemplate().opsForValue().get(key);
    }

    /**
     * * 存放
     *
     * @param key
     * @param value
     * @return
     */
    default boolean set(String key, T value) {
        try {
            getRedisTemplate().opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * *存放
     *
     * @param key
     * @param value
     * @param time
     * @return
     */
    default boolean set(String key, T value, long time) {
        try {
            if (time > 0) {
                getRedisTemplate().opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    //-------------------------------------map-------------------------------------
    @SuppressWarnings("unchecked")
    default Map<Object, T> hgetAll(String key) {
        return (Map<Object, T>) getRedisTemplate().opsForHash().entries(key);
    }

    @SuppressWarnings("unchecked")
    default T hget(String key, Object mapKey) {
        return (T) getRedisTemplate().opsForHash().get(key, mapKey);
    }

    default boolean hset(String key, Object mapKey, T value) {
        try {
            getRedisTemplate().opsForHash().put(key, mapKey, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default boolean hset(String key, Object mapKey, T value, long time) {
        try {
            getRedisTemplate().opsForHash().put(key, mapKey, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    default void hdel(String key, Object... mapKey) {
        getRedisTemplate().opsForHash().delete(key, mapKey);
    }


    default boolean hHasKey(String key, Object mapKey) {
        return getRedisTemplate().opsForHash().hasKey(key, mapKey);
    }

    //-------------------------------------------------set-----------------------------

//    default Set<T> sGet(String key) {
//        try {
//            return getRedisTemplate().opsForSet().members(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    default T pop(String key) {
//        try {
//            return getRedisTemplate().opsForSet().pop(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    default boolean sHasKey(String key, T value) {
//        try {
//            return getRedisTemplate().opsForSet().isMember(key, value);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    default long sSet(String key, T... values) {
//        try {
//            return getRedisTemplate().opsForSet().add(key, values);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    default long sSetAndTime(String key, long time, T... values) {
//        try {
//            Long count = getRedisTemplate().opsForSet().add(key, values);
//            if (time > 0)
//                expire(key, time);
//            return count;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    default long sGetSetSize(String key) {
//        try {
//            return getRedisTemplate().opsForSet().size(key);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    default long setRemove(String key, T... values) {
//        try {
//            Long count = getRedisTemplate().opsForSet().remove(key, values);
//            return count;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }

    // ===============================list=================================

    default List<T> lGet(String key, long start, long end) {
        try {
            return getRedisTemplate().opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default List<T> lGetAll(String key) {
        try {
            return getRedisTemplate().opsForList().range(key, 0, -1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default long lGetListSize(String key) {
        try {
            return getRedisTemplate().opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    default T lGetIndex(String key, long index) {
        try {
            return getRedisTemplate().opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default boolean lSet(String key, T value) {
        try {
            getRedisTemplate().opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default boolean lSet(String key, T value, long time) {
        try {
            getRedisTemplate().opsForList().rightPush(key, value);
            if (time > 0)
                expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default boolean lSet(String key, List<T> value) {
        try {
            getRedisTemplate().opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default boolean lSet(String key, List<T> value, long time) {
        try {
            getRedisTemplate().opsForList().rightPushAll(key, value);
            if (time > 0)
                expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    default T lLeftPop(String key) {
        try {
            return getRedisTemplate().opsForList().leftPop(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default boolean lRemove(String key, T value) {
        try {
            Long remove = getRedisTemplate().opsForList().remove(key, 1, value);
            if (remove > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
