package com.bose.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("user").password("pass").roles("SUPERUSER"); // ... etc.
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.httpBasic().and()
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/monitor", "/test/*", "/**/*", "/").permitAll().anyRequest()
                .authenticated();
    }


    // ... other stuff for application security

}
