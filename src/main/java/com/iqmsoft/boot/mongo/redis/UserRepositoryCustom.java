package com.iqmsoft.boot.mongo.redis;

import org.springframework.cache.annotation.CachePut;

public interface UserRepositoryCustom {
	User updateUserById(String id, User user);
	@CachePut(value = "user", key = "#username")
	User updateUserByUsername(String username, User user);
}
