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

```java
@Bean
public UserDetailsService userDetailsService(UserRepository userRepo) {
    return username -> {
        User user = userRepo.findByUsername(username);
        if (user != null) return user;

        throw new UsernameNotFoundException("User '" + username + "' not found");
    };
}
```

* loadByUsername() must never return null

#### Registering users

* [User registration controller](/src/main/java/tacos/security/RegistrationController.java)

## 5.3 Securing web requests

### 5.3.1 Securing requests

### 5.3.2 Creating a custom login page

### 5.3.3 Enabling third-party authentication

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