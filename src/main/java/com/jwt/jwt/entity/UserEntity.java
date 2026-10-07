package com.jwt.jwt.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.text.SimpleDateFormat;  
import java.util.Date;  
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Entity(name="jwt_rotation")
public class UserEntity   {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	public int id;
	@Column(updatable = false)
	public String name;
	@Column(updatable = false, unique=true)
	public String username;
	@Column(updatable = false)
	public role role;
	@Column(updatable = false)
	public String password;
	//@JsonIgnore
	@OneToMany(mappedBy = "userEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setname(String name) {
		this.name = name;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public role getRole() {
		return role;
	}
	public void setRole(role role) {
		this.role = role;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	@Override
	public String toString() {
		return "UserEntity [id=" + id + ", name=" + name + ", username=" + username + ", role=" + role + ", password="
				+ password + "]";
	}
	public UserEntity(String name, String username, com.jwt.jwt.entity.role role, String password) {
		super();
		this.name = name;
		this.username = username;
		this.role = role;
		this.password = password;
	}
	public UserEntity() {
		// TODO Auto-generated constructor stub
	}
//	public void setCreatedAt() {
//		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
//	    Date date = new Date();  
//	    System.out.println(formatter.format(date));  
//		
//	}
//	 @Override
//	  public Collection<? extends GrantedAuthority> getAuthorities() {
//	      return List.of(new SimpleGrantedAuthority(role.name()));
//	  }
//
//	@Override
//	public boolean isAccountNonExpired() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//	@Override
//	public boolean isAccountNonLocked() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//	@Override
//	public boolean isCredentialsNonExpired() {
//		// TODO Auto-generated method stub
//		return true;
//	}
//	@Override
//	public boolean isEnabled() {
//		// TODO Auto-generated method stub
//		return true;
//	}
	
}
