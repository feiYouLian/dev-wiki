package com.sjbb.core.token.service;

import com.sjbb.core.model.dto.TokenRespDTO;
import com.sjbb.core.model.dto.TokenUserDTO;
import com.sjbb.core.model.enums.Role;

public interface TokenService extends RedisService<TokenUserDTO> {

	TokenRespDTO createToken(TokenUserDTO user);

	void deleteToken(String token);

	TokenUserDTO getUserByToken(String token);

	Role getRoleByToken(String token);
}
