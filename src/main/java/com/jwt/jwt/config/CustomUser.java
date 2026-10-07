package com.jwt.jwt.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.jwt.jwt.entity.UserEntity;


public class CustomUser implements UserDetails {

		private UserEntity userEntity;

		public CustomUser(UserEntity product) {
			super();
			this.userEntity = product;
		}

		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {
			SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userEntity.getRole().toString());
			return Arrays.asList(authority);
		}

		@Override
		public String getPassword() {
			return userEntity.getPassword();
		}

		@Override
		public String getUsername() {
			return userEntity.getUsername();
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

	}

