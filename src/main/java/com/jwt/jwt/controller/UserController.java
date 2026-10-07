package com.jwt.jwt.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import com.jwt.jwt.dto.JwtResponse;
import com.jwt.jwt.dto.RefreshTokenRequest;
import com.jwt.jwt.dto.UserRequest;
import com.jwt.jwt.dto.UserResponse;
import com.jwt.jwt.dto.validTokens;
import com.jwt.jwt.entity.AuthRequest;
import com.jwt.jwt.entity.MatchReq;
import com.jwt.jwt.entity.RefreshToken;
import com.jwt.jwt.entity.UserEntity;
import com.jwt.jwt.services.JwtService;
import com.jwt.jwt.services.RefreshTokenService;
import com.jwt.jwt.services.UserService;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class UserController {
	LocalDateTime dateTime = LocalDateTime.now();
	@Autowired
	public UserService userService;
	@Autowired
	public JwtService jwtService;
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private RefreshTokenService refreshTokenService;
	@Value("${auth.max-sessions}")
	private int maxSessions;
	

	public UserController(UserService userService, JwtService jwtService) {
		super();
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@GetMapping("/hi")
	public String hi(HttpServletRequest request) {
		request.setAttribute("test", "pravar");
		System.out.println(request.getAttribute("test"));
		return "hi";
	}

	@PostMapping("/create_user")
	@ResponseStatus(HttpStatus.CREATED)
	public void createProduct(@RequestBody UserRequest userRequest) {
		System.out.println("Received request to create a user: {}" + userRequest);
		try {
			userService.createUser(userRequest);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@PreAuthorize("hasRole('ROLE_USER')")
	@GetMapping("/getusers/user")
	@ResponseStatus(HttpStatus.OK)
	public List<UserResponse> getAllProducts(// HttpServletRequest request) {
//		System.out.println("above invalidating session");
//		request.getSession().invalidate();
	) {
		return userService.getAllUsers();
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@GetMapping("/usersById/id/{id}/admin")
	@ResponseStatus(HttpStatus.OK)
	public UserEntity getUsersId(@PathVariable Integer id) {

		// userService.matchUname(Username,);
		return userService.getById(id);
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/usersById/hiadmin")
	@ResponseStatus(HttpStatus.OK)
	public String adminHi(HttpServletRequest request, @RequestBody String uname)
			throws JsonMappingException, JsonProcessingException {
		System.out.println(uname + "   passed uname");// this gives the same issue as validateWithObject() hence the
														// steps till line 103 get uname from json body
		ObjectMapper objMap = new ObjectMapper();//jackson object mapper to convert json string to json object and then get the value of the key "uname"
		JsonNode node = objMap.readTree(uname);//converting json string to json object
		String unameFromJson = node.get("uname").asText();//getting the value of the key "uname" from the json object
		System.out.println(unameFromJson);
		// ******************************************************************
		// getting token from header for this we pass HttpServletRequest request as a
		// parameter in the endpoint
		String authHeader = request.getHeader("Authorization");
		String token = null;
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
			System.out.println("*****************************************************************");
			System.out.println(token + "passed token");

		}
		// ******************************************************************
		// pass the username that we extracted and the token from the http request
		// header to match them using the following method
		boolean match = jwtService.matchUname(unameFromJson, token);
		// granting the resource only if username from token and username from body is a
		// match
		if (match) {
			return "hi admin";
		} else {
			//new RedirectView("/auth/
			// 
			
			return "the token's user and the user from the body is a mismatch ";
		}

	}
	@PostMapping("/authenticate")
	public ResponseEntity<JwtResponse> authenticate(@RequestBody AuthRequest a) {
	    try {
	        Authentication authentication = authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(a.getUserName(), a.getPassword()));

	        if (!authentication.isAuthenticated()) {
	            throw new UsernameNotFoundException("invalid user request !");
	        }

	        // for the case if refreshtoken is dead and the user comes in with correct username & password.
	        UserEntity user = userService.UsernameInDb(a.getUserName());
	        System.out.println("*****************************************************************");
	        System.out.println(user + "the entity");
	        int IdOfUser = user.getId(); // we get the id of that user
	        System.out.println("*****************************************************************");
	        System.out.println(IdOfUser + " the id of the entity");

	        // we map this user id to the token stored in the refresh_token db as the user id is the foreign key
	        int NumberOfSessions = refreshTokenService.ActiveSessions(IdOfUser);
			System.out.println("*****************************************************************");
	        System.out.println(NumberOfSessions + " the number of active sessions for this user" +IdOfUser);

	        if (NumberOfSessions >= maxSessions) {
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(JwtResponse.builder()
	                    .accessToken("you have logged on to " + maxSessions + " devices and you cannot further log in")
	                    .token("logout on one of the devices and then log in here").build());
	        }

	        // ONLY GENERATES TOKEN IF IT IS THERE IN DB
	        RefreshToken refreshToken = refreshTokenService.createRefreshToken(a.getUserName());
	        return ResponseEntity.ok(JwtResponse.builder()
	                .accessToken(jwtService.generateToken(a.getUserName(), user.getRole().toString()))
	                .token(refreshToken.getToken()).build());

	    } catch (BadCredentialsException | UsernameNotFoundException r) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(JwtResponse.builder()
	                .accessToken("bad credentials")
	                .token(null).build());
	    } catch (Exception e) {
	        e.printStackTrace();
	        // TODO Auto-generated catch block
	        System.out.println("already authenticated");
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(JwtResponse.builder()
	                .accessToken("internal server error")
	                .token(null).build());
	    }
	}


	@PostMapping("/match")
	@ResponseStatus(HttpStatus.CREATED)
	public boolean match(@RequestBody MatchReq m) throws JsonMappingException, JsonProcessingException {
		System.out.println("Entering controller method");
		return jwtService.matchUname(m.getUsername(), m.getToken());
	}
	
	@PostMapping("/validate/{token}")
	@ResponseStatus(HttpStatus.CREATED)
	public String validate(@PathVariable String token) {
		try {
			System.out.println("token : " + token);

			String a = jwtService.extractUsername(token);
			return "Valid Token";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Invalid Token";
		}
	}

	@PostMapping("/validateWithObject")
	@ResponseStatus(HttpStatus.CREATED)
	public String validateWithObject(@RequestBody String token) {
		try {
			System.out.println("token : " + token);
			/*
			 * the problem here is entire json is taken in as a string object eg: token :
			 * {"token":
			 * "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMiIsImlhdCI6MTY5OTMzMTUzNiwiZXhwIjoxNjk5NDE3OTM2fQ.WLjWrCEJKz9sv-zTFjIhRQLto4Lu3ebM2EZk1lMY_5U"}
			 * we needed only the value of the key "token" to solve this we used the jackson
			 * to json serilizer ref- https://www.youtube.com/watch?v=bV85_gIrHTc&t=155s
			 */
			ObjectMapper objMap = new ObjectMapper();
			JsonNode node = objMap.readTree(token);
			String tokenFromJson = node.get("token").asText();
			System.out.println(tokenFromJson);
			String a = jwtService.extractUsername(tokenFromJson);
			return "Valid Token";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "Invalid Token";
		}
	}

	@PostMapping("/refreshToken")
	@Transactional //Wwhenever dealing with update or delete query  add this annotation
	public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {

		try {
		    Optional<RefreshToken> optionalRefreshToken = refreshTokenService.findByToken(refreshTokenRequest.getToken());
		    System.out.println("*****************************************************************");
		    System.out.println("optionalRefreshToken "+optionalRefreshToken);
		    //finding the token in db

		    if (optionalRefreshToken.isPresent()) {
		        RefreshToken refreshToken = optionalRefreshToken.get();
		        System.out.println("*****************************************************************");
			    System.out.println("refreshToken "+refreshToken);
			    System.out.println("*****************************************************************");
			    System.out.println("refreshToken.getExpiryDate() "+refreshToken.getExpiryDate());

		        //if we find a token we store it in refreshToken variable
		    if(refreshToken.getExpiryDate().compareTo(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata")))) < 0) {
		    	if (refreshToken.isRevoked()) {
		    		// this token was already consumed by a rotation/logout and is being replayed -> reuse attack
		    		System.out.println("revoked token reuse detected - expiring all sessions");
		    		refreshTokenService.ExpireAll( refreshToken.getUserEntity().getId());//alltokens expired
		    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(JwtResponse.builder()
		    				.accessToken("suspicious refresh token reuse detected")
				            .token("all sessions have been logged out").build());
		    	} else {
		    		// token simply timed out naturally, never reused - not an attack signal
		    		System.out.println("refresh token naturally expired, no reuse detected");
		    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(JwtResponse.builder()
		    				.accessToken("this refresh token is expired")
				            .token("please login again").build());
		    	}
		    }
		    if(refreshToken.getExpiryDate().compareTo(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata")))) > 0) {
		    	//refreshTokenService.ExpireAll( refreshToken.getUserEntity().getId());
		    	UserEntity userEntity = refreshToken.getUserEntity();//get user entity (wrt that token mapped in jwt_table)
		    	System.out.println("*****************************************************************");
		    	System.out.println("userEntity "+userEntity);
		    	refreshToken.setExpiryDate(Instant.now());
		    	refreshToken.setRevoked(true);//mark this token as consumed so a later replay is detected as reuse
		    	System.out.println("the passed rt is expired");
		    	String accessToken = jwtService.generateToken(userEntity.getUsername(), userEntity.getRole().toString());//generate the new access token
		    	System.out.println("*****************************************************************");
		    	System.out.println("accessToken "+accessToken);
		    	System.out.println("*****************************************************************");
		    	System.out.println("userEntity.getUsername() "+userEntity.getUsername());
		    	RefreshToken refreshTokenNew = refreshTokenService.createRefreshToken(userEntity.getUsername());//generate new refresh token return
		    	System.out.println("*****************************************************************");
		    	System.out.println("refreshTokenNew "+refreshTokenNew.getToken());
		    	return ResponseEntity.ok(JwtResponse.builder().accessToken(accessToken).token(refreshTokenNew.getToken()).build());

		        }else {
		        	throw new RuntimeException("Refresh token has expired!");
		        }} else {
		        	throw new RuntimeException("Refresh token is not in the database!");}}
		catch (Exception e) {
	        e.printStackTrace();
	        // TODO Auto-generated catch block
	        System.out.println("already authenticated");
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(JwtResponse.builder()
	                .accessToken("invalid refresh token")
	                .token(null).build());
	    }
	}

	// returns live refresh tokens, which are enough to take over a session - admin only
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/tokenByName")
	public validTokens test(@RequestBody String name) {
		System.out.println("*****************************************************************");
	    System.out.println("In tokenByName endpoint with name "+name);
		int id = userService.getidFromUsername(name);
		System.out.println("*****************************************************************");
	    System.out.println("id "+id+ " for name "+name);
		return new validTokens(id, refreshTokenService.getActiveTokenUUIDs(id));
	}

	@GetMapping("/redirect")
	public RedirectView performLogout() {
		
		return new RedirectView("/hi");
	}

}


