package com.sjbb.core.token.service.impl;

import com.sjbb.core.constant.CommonConst;
import com.sjbb.core.model.dto.TokenRespDTO;
import com.sjbb.core.model.dto.TokenUserDTO;
import com.sjbb.core.model.enums.Role;
import com.sjbb.core.token.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenServiceImpl implements TokenService {

    @Autowired
    RedisTemplate<String, TokenUserDTO> redisTemplate;


    private String buildKey(String key) {
        return CommonConst.TOKEN_KEY_PRE + key;
    }

    @Override
    public RedisTemplate<String, TokenUserDTO> getRedisTemplate() {
        return redisTemplate;
    }

    @Override
    public TokenRespDTO createToken(TokenUserDTO user) {
        Integer expiresIn;
        if (Role.DEVICE.equals(user.getRole())) {
            expiresIn = 3 * CommonConst.EXPIREIN;
        } else if (Role.USER.equals(user.getRole())) {
            expiresIn = CommonConst.EXPIREIN;
        } else {
            expiresIn = CommonConst.EXPIREIN;
        }
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        this.set(buildKey(token), user, expiresIn + CommonConst.EXPIREIN_DEVIATION);

        TokenRespDTO tokenResp = new TokenRespDTO();
        tokenResp.setUserId(user.getUserId());
        tokenResp.setRole(user.getRole());
        tokenResp.setToken(token);
        tokenResp.setExpiresIn(expiresIn);
        return tokenResp;
    }

    @Override
    public void deleteToken(String token) {
        this.del(buildKey(token));
    }

    @Override
    public TokenUserDTO getUserByToken(String token) {
        return this.get(buildKey(token));
    }

    @Override
    public Role getRoleByToken(String token) {
        TokenUserDTO tokenUserDTO = this.get(buildKey(token));
        return tokenUserDTO == null ? null : tokenUserDTO.getRole();
    }

}
