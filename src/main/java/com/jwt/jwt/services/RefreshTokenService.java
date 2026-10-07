package com.jwt.jwt.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jwt.jwt.entity.RefreshToken;
import com.jwt.jwt.entity.UserEntity;
import com.jwt.jwt.repository.JwtRepository;
import com.jwt.jwt.repository.RefreshTokenRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtRepository userInfoRepository;
    @Value("${auth.refresh-token.ttl-ms}")
    private long refreshTokenTtlMs;

	public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userEntity(userInfoRepository.findByUsername(username))
                .token(UUID.randomUUID().toString())  // a random id generated that is used to generate refresh token.
                .generationTime(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata"))))
                .expiryDate(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata"))).plusMillis(refreshTokenTtlMs))//default 20 mins
                .build();
        return refreshTokenRepository.save(refreshToken);
    }



	public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public String verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata")))) < 0) {
            refreshTokenRepository.delete(token);//checks weather the refresh token is expired if yes deletes that entity from the db as well
            throw new RuntimeException(token.getToken() + " Refresh token was expired. Please make a new signin request");
            
        }
        return token.toString();// if good token return token
    }
    public RefreshToken GetTokenByUsername(String Username){
    	UserEntity u = userInfoRepository.findByUsername(Username);
    	System.out.println(u);
    	int idToSearch = u.getId();
    	System.out.println(idToSearch+"id this");
    	RefreshToken r = refreshTokenRepository.findTokenByUserId(idToSearch);
    	System.out.println(r +" "+ "RT" );
    	return r;
    	
    	
    }
    public RefreshToken GetTokenById(int id) {
    	return refreshTokenRepository.findTokenByUserId(id);
    }
    public void DeleteTokenById(int id) {
	   refreshTokenRepository.deleteById(id);
	   System.out.println("delete success");
   }
    public RefreshToken saveRt(RefreshToken r) {
    	return refreshTokenRepository.save(r);
    }
    public int ExpireAll(int id) {
    	return refreshTokenRepository.upQuery(id);
    }
    public int ActiveSessions(int idOfUser){
    	return refreshTokenRepository.findActiveTokensByUserId(idOfUser);
    }

    public ArrayList<String> getActiveTokenUUIDs(int id) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserEntity_IdAndExpiryDateAfter(id, Instant.now(Clock.system(ZoneId.of("Asia/Kolkata"))));
        ArrayList<String> uuids = new ArrayList<>();
        for (RefreshToken rt : activeTokens) {
            uuids.add(rt.getToken());
        }
        return uuids;
    }

}