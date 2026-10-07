package com.jwt.jwt.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jwt.jwt.entity.UserEntity;
public interface JwtRepository extends JpaRepository<UserEntity, Integer> {
	UserEntity findByUsername(String username);
}
