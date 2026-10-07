package com.jwt.jwt.services;

import java.util.List;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.jwt.jwt.dto.UserRequest;
import com.jwt.jwt.dto.UserResponse;
import com.jwt.jwt.repository.JwtRepository;

import jakarta.servlet.http.HttpServletRequest;

import com.jwt.jwt.entity.*;
import com.jwt.jwt.services.JwtService;

@Component
@Service
public class UserService {
	public JwtRepository jrepo;
	@Autowired
	private PasswordEncoder passwordEncoder;
	JwtService jwtService;
	private AuthenticationManager authenticationManager;

	public UserService(JwtRepository jrepo, JwtService jwtService) {
		super();
		this.jrepo = jrepo;
		this.jwtService = jwtService;

	}

	public List<UserResponse> getAllUsers() {
		List<UserEntity> userList = jrepo.findAll();
		return userList.stream().map(this::mapToUserResponse).toList();
	}
	public int getidFromUsername(String username) {
		UserEntity user = jrepo.findByUsername(username);
		return user.getId();
	}

	public UserResponse mapToUserResponse(UserEntity userEntity) {
		UserResponse ur = new UserResponse();
		ur.setId(userEntity.getId());
		ur.setName(userEntity.getName());
		ur.setRole(userEntity.getRole());
		ur.setUsername(userEntity.getUsername());
		ur.setPassword(userEntity.getPassword());
		return ur;
	}

	public UserEntity getById(Integer tid) {
		Optional<UserEntity> findById = jrepo.findById(tid);
		if (findById.isPresent()) {
			return findById.get();
		}
		return null;
	}

	public UserEntity createUser(UserRequest userRequest)throws Exception {
try {UserEntity user = new UserEntity();
user.setname(userRequest.getName());
user.setUsername(userRequest.getUsername()); // Set username
user.setRole(userRequest.getRole()); // Set role
String encodedPassword = passwordEncoder.encode(userRequest.getPassword());
user.setPassword(encodedPassword);
return jrepo.save(user);} 
catch(Exception e) 
{System.out.println("username must be unique");return null;}
		

	}
/*
	public boolean matchUname(String username, String token) {
		System.out.println("Entering match service method");
		// String authHeader = request.getHeader("Authorization");
		// String token = null;

		System.out.println("line77");
//    if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        token = authHeader.substring(7);
//        usernameInToken = jwtService.extractUsername(token);
//    }
		try {
			
			if (jwtService.extractUsername(token) == null) {
				System.out.println("in if");
				return false;
			}
		} catch (Exception ex) {
//			ex.printStackTrace();
			return false;
		}
		
		System.out.println("in else");
		String usernameInToken = jwtService.extractUsername(token);
		System.out.println(usernameInToken);

		if (usernameInToken.equals(username)) {
			System.out.println("in same check");
			System.out.println(usernameInToken + " " + "tok");
			System.out.println(username + " " + "uname");
			return true;
		} else {
			System.out.println("line 89");
			return false;
		}
		// System.out.println("line 89");

	}*/
	public UserEntity UsernameInDb(String usernameInReq) {
		return jrepo.findByUsername(usernameInReq);
	}

}
