/*
 * Copyright 2021 Koen Serneels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.error.awsddns;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class AppWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

	@Value("${awsddns.username}")
	private String username;
	@Value("${awsddns.password}")
	private String password;
	@Value("${awsddns.admin.username}")
	private String adminUsername;
	@Value("${awsddns.admin.password}")
	private String adminPassword;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser(username).password("{noop}" + password).authorities("ROLE_USER");
		auth.inMemoryAuthentication().withUser(adminUsername).password("{noop}" + adminPassword).authorities("ROLE_ADMIN");
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		UrlParamAuthenticationFilter urlParamAuthenticationFilter = new UrlParamAuthenticationFilter();
		urlParamAuthenticationFilter.setAuthenticationManager(super.authenticationManager());

		http.httpBasic().realmName("AWSDDNS").and().authorizeRequests().antMatchers("/actuator/**").hasRole("ADMIN").and().csrf().disable()
				.addFilterBefore(urlParamAuthenticationFilter, BasicAuthenticationFilter.class).authorizeRequests().anyRequest().authenticated();
	}
}
