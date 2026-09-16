package com.sjbb.core.token;

import com.sjbb.core.model.dto.TokenUserDTO;

import java.util.HashMap;
import java.util.Map;

public class TokenHolder {
	public static final ThreadLocal<Map<String, Object>> threadLocal = new ThreadLocal<>();

	public static final String CONTEXT_TOKEN_USER = "user";
	public static final String CONTEXT_TOKEN = "token";
	public static final String IP = "ip";

	public static void set(String key, Object value) {
		Map<String, Object> map = threadLocal.get();
		if (map == null) {
			map = new HashMap<>();
			threadLocal.set(map);
		}
		map.put(key, value);
	}

	public static Object get(String key) {
		Map<String, Object> map = threadLocal.get();
		if (map == null) {
			map = new HashMap<>();
			threadLocal.set(map);
		}
		return map.get(key);
	}

	public static String getToken() {
		return (String) get(CONTEXT_TOKEN);
	}

	public static TokenUserDTO getUser() {
		return (TokenUserDTO) get(CONTEXT_TOKEN_USER);
	}

	public static String getIp() {
		return (String) get(IP);
	}

	public static void setUser(TokenUserDTO user) {
		set(CONTEXT_TOKEN_USER, user);
	}

	public static void setToken(String token) {
		set(CONTEXT_TOKEN, token);
	}

	public static void setIp(String ip) {
		set(IP, ip);
	}

	public static void remove() {
		threadLocal.remove();
	}
}
