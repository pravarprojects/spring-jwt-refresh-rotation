package com.jwt.jwt.config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.jwt.jwt.services.LogoutHandlerService;
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig  {
	 @Autowired
	 private JwtAuthFilter authFilter;
	 @Autowired
	 private PasswordEncoder passwordEncoder;
	 @Autowired
	 private LogoutHandlerService logoutHandler;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	@Bean
	public UserDetailsService getDetailsService() {
		return new CustomUserDetailsService();
	}
	// hardcoded users below not in db
//	@Bean
//	UserDetailsService userDetailService() {
//		UserDetails user = User.withUsername("pravar").password(passwordEncoder().encode("pravar")).roles("USER").build();
//		UserDetails admin = User.withUsername("admin").password(passwordEncoder().encode("admin")).roles("ADMIN").build();
//		InMemoryUserDetailsManager i =new InMemoryUserDetailsManager(user,admin);
//		return i;
//	}
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(getDetailsService());
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder()); 
		return daoAuthenticationProvider;
	}
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf->csrf.disable()).
			authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/hi","/create_user","/authenticate","/match","/validate/{token}","/validateWithObject","/refreshToken","/logout","/redirect").permitAll()
				.anyRequest().authenticated()
				).authenticationProvider(authenticationProvider())
			.addFilterBefore( authFilter,UsernamePasswordAuthenticationFilter.class)
			 .logout(logout ->
             logout.logoutUrl("/logout")
                     .addLogoutHandler(logoutHandler)
                     .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
     );//.formLogin(Customizer.withDefaults());
		return http.build();//jjj
	}
	   @Bean
	    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
	        return config.getAuthenticationManager();
	    }
	 
	
}