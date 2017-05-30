package com.iqmsoft.boot.mongo.redis;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

	@Cacheable(value = "user", key = "#username")
	User findByUsername(String username);
	List<User> findByFirstname(String firstname);
	List<User> findByLastname(String lastame);
	@CacheEvict(value = "user", key = "#username")
	void deleteByUsername(String username);
	Long countByUsername(String username);

}
