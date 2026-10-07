
package com.jwt.jwt.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.jwt.jwt.entity.RefreshToken;
import java.time.Instant;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer>{


	Optional<RefreshToken> findByToken(String token);

	List<RefreshToken> findByUserEntity_IdAndExpiryDateAfter(Integer id, Instant now);

	@Query(value="SELECT t.* FROM refresh_rotation t INNER JOIN jwt_rotation u ON t.user_entity_id = u.id WHERE u.id = ?1 ORDER BY t.id DESC LIMIT 1", nativeQuery = true)
	RefreshToken findTokenByUserId(Integer id);

	@Query(value="SELECT count(*) FROM refresh_rotation t  JOIN jwt_rotation u ON t.user_entity_id = u.id WHERE t.expiry_date > NOW() AND u.id = ?1"
			, nativeQuery = true)
	int findActiveTokensByUserId(Integer id);//returns the number of active sessions associated to a user by taking user id

	@Transactional//add this while update or delete
	@Modifying//add this while update or delete
	@Query(value="UPDATE refresh_rotation SET expiry_date = NOW(), revoked = true WHERE expiry_date > NOW() AND user_entity_id = ?1", nativeQuery = true)
	int upQuery(int id);//updates the expiry time of the valid tokens to current time and marks them revoked

}
