package tacos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    // in 5th ed. it extends `WebSecurityConfigurerAdapter`

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // depreciated, cannot used in Spring Boot 4 (어리석은 자여...) 
        // return http
        //     .authorizeRequests()
        //         .antMatchers("/design", "/orders").hasRole("USER")
        //         .antMatchers("/", "/**").permitAll()
        //     .and()
        //     .build();

        // 참조 https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#authorizing-endpoints
        // 이 버전부터 람다 표현식으로 설계하도록 바뀌었다
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/design", "/orders").hasRole("USER")
                .requestMatchers("/", "/**").permitAll()
            ).formLogin(formLogin -> formLogin
                .loginPage("/login")
                // 아래와 같은 식으로 로그인 프로세스의 path, fieldname 변경 가능
                // .loginProcessingUrl("/authenticate")
                // .usernameParameter("user")
                // .passwordParameter("pwd")
                .defaultSuccessUrl("/design", true)
            );
        
        return http.build();

    }

}
