# 5 Securing Spring

1. Enabling Spring Security
2. Configuring authentication
  1. In-memory user details service
  2. Customizing user authentication
3. Securing web requests
  1. Securing requests
  2. Creating a custom login page
  3. Enabling third-party authentication
  4. Preventing cross-site request forgery
4. Applying method-level security
5. Knowing your user

## 5.1 Enabling Spring Security

* add Dependency to project build config file
* [Dependency](/pom.xml)

* `user` with password given on log entry

* add security starter will do...
  * all HTTP req requires authentication
  * no roles, no authorities
  * authentication w/ simple login page
  * only one user `user`

## 5.2 Configuring authentication

* barebones configuration class

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // add more security configuration here...

}
```

* Spring Security
  * BCryptPasswordEncoder: bcrypt strong hashing
  * NoOpPasswordEncoder: no encoding
  * Pbkdf2PasswordEncoder: PBKDF2 encrypt
  * SCryptPasswordEncoder: Scrypt hasing
  * StandardPasswordEncoder: SHA-256 hashing
* password -> `passwordEncoder.matches()`

* Handle more users -> user store
  * `UserDetailsService` bean is needed

```java
public interface UserDetailService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

* Spring Security's out-of-the-box implementations
  * in-memory user store
  * JDBC user store
  * LDAP user store
* custom implementations

### 5.2.1 In-memory user details service

* Defining in-memory users in security config
* convenient for testing purposes
* or, for very simple application with fixed, small number of users
* if users need to be changed: rebuild, redeploy the app

```java
@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    // adding two "buzz:password", "woody:password" users
    List<UserDetails> usersList = new ArrayList<>();
    usersList.add(new User(
        "buzz", encoder.encode("password"), Arrays.asList(new SimpleGrantedAuthrity("ROLE_USER"))
    ));
    usersList.add(new User(
        "woody", encoder.encode("password"), Arrays.asList(new SimpleGrantedAuthrity("ROLE_USER"))
    ));
}
```

### 5.2.2 Customizing user authentication

* Using Spring Data JPA?
  * JDBC authentication
  * better yet, leverage Spring Data JPA repo

#### Defining the user domain and persistence

* Defining a user entity
  * [User](/src/main/java/tacos/User.java)
  * Detailed for the domain (e.g. address infos)
  * and also implementing `UserDetails` interface
    * getAuthorities(): authorities granted
    * is*: is user enabled, locked or expired

```java
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);
}
```

#### Creating a user details service

* `UserDetailService` interface with single `loadUserByUsername()` method
  * functional interface
  * so... can be implemented by lambda

```java
@Bean
public UserDetailsService userDetailsService(UserRepository userRepo) {
    // 람다 표현식으로 쉽게 구현하는 UserDetailService
    // (UserRepo를 받아서 처리)
    return username -> {
        User user = userRepo.findByUsername(username);
        if (user != null) return user;

        throw new UsernameNotFoundException("User '" + username + "' not found");
    };
}
```

* loadByUsername() must never return null

#### Registering users

* Spring Security handles many... but not so much for registration of user
* use Spring MVC for that

* [User registration controller](/src/main/java/tacos/security/RegistrationController.java)

* 여기까지 - 
  * by default, all requests require auth (include registration)
  * let's fix that

## 5.3 Securing web requests

* Pages should be accessible even for unauthenticated users:
  * home
  * login
  * registration

* Security rules are configured with `SecurityFilterChain` beans

```java
// 가장 단순한 SecurityFilterChain 예시
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.build();
}
```

* `filterChain()` method <- `HttpSecurity` object
  * `HttpSecurity` acts as builder
  * after set up, call `build()` -> SFC returned

* configurations with HttpSecurity:
  * Security conditions
  * custom login page
  * log outs
  * CSRF protection
  * ...

* [(참고할만한 공식 블로그 문서)](https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter)

### 5.3.1 Securing requests

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            // only "USER" can access design, orders page
            .antMatchers("/design", "/orders").hasRole("USER")
            .antMatchers("/", "/**").permitAll()
        .and()
        .build();
}
```

* `authorizeRequests()` -> `ExpressionUrlAuthorizationConfigurer.ExpressionInterceptUrlRegistry` -> path settings
* rules:
  * "/design" and "/orders": `ROLE_USER` ("ROLE" is assumed with `hasRole()` so omit it)
  * other pages: all

* more than `hasRole()` and `permitAll()`...
* Configuration methods
  * `access(String)`: use SpEL expression
  * `anonymous()`
  * `authenticated()`
  * `denyAll()`
  * `fullyAuthenticated()`
  * `hasAnyAuthority(String...)`
  * `hasAnyRole(String...)`
  * `hasAuthority(String)`
  * `hasIpAddress(String)`
  * `hasRole(String)`
  * `not()`
  * `permitAll()`
  * `rememberMe()`

* Spring Security extensions to SpEL (Spring Expression Language)
  * authentication
  * denyAll
  * hasAnyAuthority(String...authorities)
  * hasAnyRole(String...roles)
  * hasAuthority(String authority)
  * hasPermission(Object target, Object permission)
  * hasPermission(Serializable targetId, String targetType, Object permission)
  * hasRole(String role)
  * hasIpAddress(String ipAddress)
  * isAnonymous()
  * isAuthenticated()
  * isFullyAuthenticated()
  * isRememberMe()
  * permitAll
  * principal

* like this:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .antMatchers("/design", "/orders").access("hasRole('USER')")
            .antMatchers("/", "/**").access("permitAll()")
        .and()
        .build();
}
```

* SpEL extreme usage:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .antMatchers("/design", "/orders")
                .access("hasRole('USER') && "
                    "T(java.util.Calendar).getInstance().get(" +
                    "T(java.util.Calendar).DAY_OF_WEEK) == " +
                    "T(java.util.Calendar).TUESDAY")
            .antMatchers("/", "/**").access("permitAll")
        .and()
        .build();
}
```

### 5.3.2 Creating a custom login page

* Replacing login page, telling Spring Security what path is custom login page
  * by calling `formLogin()` on `HttpSecurity` Object

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .antMatchers("/design", "/orders").hasRole("USER")
            .antMatchers("/", "/**").permitAll()
        .and()
            .formLogin()
                .loginPage("/login")
        .and()
        .build();
}
```

* [Login page template](/src/main/resources/templates/login.html)

* Login process can be customized with these:

```java
// ...
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/authenticate")
                .usernameParameter("user")
                .passwordParameter("pwd")
                .defaultSuccessUrl("/design", true)
```

### 5.3.3 Enabling third-party authentication

* "Sign in with ..."
* OAuth2, or OpenID Connect (OIDC)
  * [OAuth2](https://oauth.net/2/)
  * [OpenID Connect](https://openid.net/developers/how-connect-works/)

* Spring Security supports...
  * Facebook, Google, GitHub, Okta (out-of-the-box)
  * other clients (extra properties)

```
spring:
  security:
    oauth2:
      client:
        registration:
          <oauth2 or openid provider name>:
            cliendId: <client id>
            clientSecret: <client secret>
            scope: <comma-separated list of requested scopes>
```

### 5.3.4 Preventing cross-site request forgery

## 5.4 Applying method-level security

## 5.5 Knowing your user

## Summary

* Spring Security autoconfiguration
* User Details -> (...)
  * relational DBs
  * LDAP
  * custom implementations
* <-> CSRF attacks
* `SecurityContext` object / `@AuthenticationPrincipal`