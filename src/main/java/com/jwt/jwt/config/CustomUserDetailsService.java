package com.jwt.jwt.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import com.jwt.jwt.config.CustomUser;
import com.jwt.jwt.entity.UserEntity;
import com.jwt.jwt.repository.JwtRepository;


@Component
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private JwtRepository jrepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		UserEntity user = jrepo.findByUsername(username);

		if (user == null) {
			throw new UsernameNotFoundException("user name not found");
		} else {
			return new CustomUser(user);
		}
		
	}

}