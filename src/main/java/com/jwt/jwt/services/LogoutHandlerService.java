package com.jwt.jwt.services;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import com.jwt.jwt.entity.RefreshToken;
import com.jwt.jwt.entity.UserEntity;
import com.jwt.jwt.repository.JwtRepository;
import com.jwt.jwt.repository.RefreshTokenRepository;

import io.jsonwebtoken.ExpiredJwtException;

@Service
public class LogoutHandlerService implements LogoutHandler {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RefreshTokenService RtService;

	@Autowired
	private RefreshTokenRepository RtRepo;

	@Autowired
	private JwtRepository jwtRepo;

	@Transactional
	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		String authHeader = request.getHeader("Authorization");
		String tokenValue = null;
		String username = null;

		if (authHeader != null && authHeader.startsWith("Bearer")) {
			tokenValue = authHeader.substring(7);
			System.out.println("tokenValue " + tokenValue);
			RefreshToken rToken = RtRepo.findByToken(tokenValue).get();

			if (rToken.getExpiryDate().compareTo(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata")))) > 0) {
				System.out.println("in valid token");
				// valid token
				rToken.setExpiryDate(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata"))));
				rToken.setRevoked(true);//mark consumed so a later replay of this token is detected as reuse
				SecurityContextHolder.clearContext();
				System.out.println("Logout done, token expired");
			} else if (rToken.isRevoked()) {
				// this token was already consumed (rotated or logged out) and is being replayed -> reuse attack
				System.out.println("Malicious user - revoked token reuse detected");
				RtRepo.upQuery(rToken.getUserEntity().getId());
				System.out.println("Logged out of all devices");
			} else {
				// token simply timed out naturally before logout was called - not an attack signal
				System.out.println("Token already naturally expired, no reuse detected");
			}

//			try {
//				username = jwtService.extractUsername(tokenValue); // getting username from the current token
//			} catch (ExpiredJwtException ex) {
//				System.out.println("Token expired");
//			}
//
//			System.out.println("username " + username);
//			RefreshToken rToken = RtService.GetTokenByUsername(username); // using the above username to get all refresh
//			System.out.println("rToken " + rToken); // tokens associated with it
//
//			if (rToken != null) {
//				int idOfUserMappedToToken = Integer.parseInt(rToken.getUserEntity().toString());
//				System.out.println("idOfUserMappedToToken " + idOfUserMappedToToken);
//				UserEntity userInDb = jwtRepo.findByUsername(username);
//				System.out.println("userInDb " + userInDb);
//				int idOfUsername = userInDb.getId();
//				System.out.println("idOfUsername " + idOfUsername);
//
//				if (idOfUserMappedToToken == idOfUsername) {
//					System.out.println("matched id of token & username");
//					if (rToken.getExpiryDate().compareTo(Instant.now()) > 0) {
//						System.out.println("in valid token");
//						// valid token
//						rToken.setExpiryDate(Instant.now());
//						SecurityContextHolder.clearContext();
//						System.out.println("Logout done, token expired");
//					} else {
//						System.out.println("Good user but Token he sent is expired");
//					}
//				} else {
//					System.out.println("Malicious user spotted");
//					RtRepo.upQuery(idOfUserMappedToToken);
//					System.out.println("Logged out of all devices");
//				}
//			} else {
//				System.out.println("No Refresh token reference id found in the db");
//			}
		} else {
			System.out.println("Token not found in request header. Cannot logout.");
		}
	}
}

/*
 * package com.jwt.jwt.services;
 * 
 * import java.time.Instant; import java.util.Optional;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.security.core.Authentication; import
 * org.springframework.security.core.context.SecurityContextHolder; import
 * org.springframework.security.web.authentication.logout.LogoutHandler; import
 * org.springframework.stereotype.Service; import
 * org.springframework.web.bind.annotation.RequestBody; import
 * com.jwt.jwt.entity.UserEntity; import com.jwt.jwt.entity.RefreshToken; import
 * jakarta.servlet.http.HttpServletRequest; import
 * jakarta.servlet.http.HttpServletResponse; import java.util.ArrayList; import
 * com.jwt.jwt.repository.JwtRepository; import
 * com.jwt.jwt.repository.RefreshTokenRepository;
 * 
 * @Service public class LogoutHandlerService implements LogoutHandler{
 * 
 * @Autowired private JwtService jwtService;
 * 
 * @Autowired private RefreshTokenService RtService;
 * 
 * @Autowired private RefreshTokenRepository RtRepo;
 * 
 * @Autowired private JwtRepository jwtRepo;
 * 
 * 
 * public void logout(HttpServletRequest request, HttpServletResponse response,
 * Authentication authentication) { RefreshToken token; String Username;
 * 
 * 
 * // TODO Auto-generated method stub] /*String authHeader =
 * request.getHeader("Authorization"); String token = null; String username =
 * null; if (authHeader != null && authHeader.startsWith("Bearer ")) { token =
 * authHeader.substring(7); username = jwtService.extractUsername(token);//
 * getting username from current token RefreshToken rToken=
 * RtService.GetTokenByUsername(username); // using above username to get get
 * all refresh tokens associated to it if(rToken != null) {//checking weather
 * there exists a refresh token id in the refresh token db wrt that name
 * RtRepo.deleteById(rToken.getId()); SecurityContextHolder.clearContext();
 * System.out.println("logout done token deleted"); }else
 * {System.out.println("no Refresh token refrence id found in db");} }else
 * {System.out.println(" problem with the token  ");}
 * 
 * if (token != null) { OurLogout(token, Username); } else {
 * System.out.println("No Refresh token reference id found in the db"); } }
 * 
 * public void OurLogout( RefreshToken token, String Username) { int
 * idOfUserMappedToToken = Integer.parseInt(token.getUserEntity().toString());
 * UserEntity userInDb = jwtRepo.findByUsername(Username); int idOfUsername =
 * userInDb.getId(); if (idOfUserMappedToToken==idOfUsername) { if
 * (token.getExpiryDate().compareTo(Instant.now()) > 0) { //valid token
 * token.setExpiryDate(Instant.now()); SecurityContextHolder.clearContext();
 * System.out.println("logout done token deleted"); }else {
 * System.out.println("Good user but Token is expired"); } }else {
 * System.out.println("Malicious user spotted");
 * RtRepo.upQuery(idOfUserMappedToToken);
 * System.out.println("logged out of all devices"); } }
 * 
 * }
 */
