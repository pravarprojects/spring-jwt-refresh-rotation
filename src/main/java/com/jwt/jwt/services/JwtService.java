package com.jwt.jwt.services;
import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class JwtService {

	@Value("${token.secret.key}")
	private String secret;

	@Value("${auth.access-token.ttl-ms}")
	private long accessTokenTtlMs;


  public String extractUsername(String token) {
      return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
      return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
      final Claims claims = extractAllClaims(token);
      return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
      return Jwts
              .parserBuilder()
              .setSigningKey(getSignKey())
              .build()
              .parseClaimsJws(token)
              .getBody();
  }

//  private Boolean isTokenExpired(String token) {
//      return false;
//  }

  private Boolean isTokenExpired(String token) {
      return extractExpiration(token).before(new Date());
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
      final String username = extractUsername(token);
      return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }


  public String generateToken(String username, String role){
      Map<String,Object> claims=new HashMap<>();
      claims.put("role", role);
      return createToken(claims,username);
  }

  private String createToken(Map<String, Object> claims, String username) {
      return Jwts.builder()
              .setClaims(claims)
              .setSubject(username)
              .setIssuedAt(new Date(System.currentTimeMillis()))
              .setExpiration(new Date(System.currentTimeMillis()+accessTokenTtlMs))//default 10 mins
              .signWith(getSignKey(), SignatureAlgorithm.HS256).compact();
  }

  private Key getSignKey() {
      byte[] keyBytes= Decoders.BASE64.decode(secret);
      return Keys.hmacShaKeyFor(keyBytes);
  }
//  public String refreshToken(String token) {
//      // Extract the username and any other claims if needed
//      String username = extractUsername(token);
//
//      // Create new claims or update the existing ones, e.g., a longer expiration time
//      Map<String, Object> updatedClaims = new HashMap<>();
//      // You can add more claims or adjust the expiration time here
//      updatedClaims.put("customClaim", "someValue");
//      Date newExpiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24); // New expiration time (e.g., 1 day)
//
//      // Create a new token with the updated claims
//      return createToken(updatedClaims, username);
//  }
//  public void getToken() {
//	    //String token = null;
//	    var authentication = SecurityContextHolder.getContext().getAuthentication();
//	    System.out.println(authentication);
////	    if (authentication != null) {
////	      token = ;
////	    }
////	    return token;
//	  }
	public boolean matchUname(String username, String token) {
		System.out.println("Entering match service method");
		// String authHeader = request.getHeader("Authorization");
		// String token = null;

		System.out.println("line110");
//    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        token = authHeader.substring(7);
//        usernameInToken = jwtService.extractUsername(token);
//    }
		try {
			
			if (this.extractUsername(token) == null) {
				System.out.println("in if");
				return false;
			}
		} catch (Exception ex) {
//			ex.printStackTrace();
			return false;
		}
		
		System.out.println("in else");
		String usernameInToken = this.extractUsername(token);
		System.out.println("usernameInToken "+usernameInToken);

		if (usernameInToken.equals(username)) {
			System.out.println("in same check");
			System.out.println(usernameInToken + " " + "tok");
			System.out.println(username + " " + "uname");
			return true;
		} else {
			System.out.println("line 136");
			return false;
		}
		// System.out.println("line 89");

	}

  
  
}