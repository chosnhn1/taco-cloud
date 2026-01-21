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

* with OAuth API...

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .mvcMatchers("/design", "/orders").hasRole("USER")
            .anyRequest().permitAll()
        .and()
            .formLogin()
                .loginPage("/login")
        .and()
            .oauth2Login()
        // ...
        .and()
        .build();
}
```

```html
<a th:href="/oauth2/authorization/facebook">Sign in with Facebook</a>
```

### 5.3.4 Preventing cross-site request forgery

* Cross-site request forgery (CSRF)
* (뭔지 익숙하지만 한번 더)
  * User -> Forged site -> false request
  * prevent: Genuine site gives user a CSRF token, checks it when posted

* Spring Security: built-in protection / easy implementation

```html
<input type="hidden" name="_csrf" th:value="${_csrf.token}"/>
```

* Using JSP tag library or Thymeleaf: even this is not necessary
  * Thymeleaf: `<form>` with thymeleaf attribute: applied automatically

* If you (somehow) doesn't want CSRF support:

```java
    .and()
        .csrf()
            .disable()
    // ...
```

## 5.4 Applying method-level security

* Web-request level Security
* but sometimes verifying user in method-level comes in handy

* example: "clear all orders" method for management purpose 

```java
public void deleteAllOrders() {
    orderRepository.deleteAll();
}
```

```java
@Controller
@RequestMapping("/admin")
public class AdminController {
    private OrderAdminService adminService;

    public AdminController(OrderAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/deleteOrders")
    public String deleteAllOrders() {
        adminService.deleteAllOrders();
        return "redirect:/admin";
    }
}
```

* using SecurityConfig for this:

```java
    .authorizeRequests()
        // ...
        .antMatchers(HttpMethod.POST, "/admin/**")
            .access("hasRole('ADMIN')")
```

* But what if some other controllers also use `deleteAllOrders()`?
  * secure all matchers
  * , or, how about this?

```java
@PreAuthorize("hasRole('ADMIN')")   // PreAuthorize takes SpEL
public void deleteAllOrders() {     // SpEL -> true -> allow to evoke method
    orderRepository.deleteAll();
}
```

* to use `@PreAuthorize` it should be configured

```java
@Configuration
@EnableGlobalMethodSecurity   // <--
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    //...
}
```

* `@PostAuthorize`: works almost same as `@PreAuthorize` 
  * but expression will be evaluate after method is evoked and returns
  * , which returned value can be used whether to permit the method (!)

* example: method fetches an order by order id
  * who can call this?: ADMIN, and USER who has this order

```java
@PostAuthorize("hasRole('ADMIN') || " +
    "returnObject.user.username == authentication.name")    // <--
public TacoOrder getOrder(long id) {
    // ...
}
```

## 5.5 Knowing your user

* It's important to know who the users of the application are: *tailor UX*

* e.g. prepopulating TacoOrder with User info
  * (e-커머스라면 '기본배송지' 같은 기능들)
* "User - other entities" relations

* (참고: 5판은 `Order`를 쓰지만 6판은 `TacoOrder`로 정의한다)

```java
@Data
@Entity
@Table(name="Taco_Order")
public class TacoOrder implements Serializable {

    // User와의 M-1 관계
    @ManyToOne
    private User user;

}
```

* `processOrder()` in `OrderController`: save TacoOrder, so should determine who is user

* the way to determine who is the user:
  * Inject `java.security.Principal` into ctrlr method
  * Inject `org.springframework.core.Authentication` into ctrlr method
  * Use `...core.context.SecurityContextHolder` to get security context
  * Inject `@AuthenticationPrincipal` annotated method param

* example:
  * #1: works fine, but unrelated codes to security
  * #2: also works fine, but casting
  * #3: more feasible approach

```java
// e.g. 1
@PostMapping
public String processOrder(@Valid TacoOrder order, Errors errors, SessionStatus sessionStatus, Principal principal) {
    // ...
    User user = userRepository.findByUsername(principal.getName());
    order.setUser(user);
    // ...
}
```

```java
// e.g. 2
@PostMapping
public String processOrder(@Valid TacoOrder order, Errors errors, SessionStatus sessionStatus, Authentication authentication) {
    // ...
    User user = (User) authentication.getPrincipal();
    order.setUser(user);
    // ...
}
```

```java
@PostMapping
public String processOrder(@Valid TacoOrder order, Errors errors, SessionStatus sessionStatus, @AuthenticationPrincipal User user) {
    // ...
    order.setUser(user);
    // ...
}
```

* What's nice:
  * cast not required
  * limits securitiy-specific code to annotation

```java
// 이것도 가능은 하다 (messy, security-specifically thick)
// good for: used anywhere, suitable for use in lower level of the code
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
User user = (User) authentication.getPrincipal();
```

## Summary

* Spring Security autoconfiguration
* User Details -> (...)
  * relational DBs
  * LDAP
  * custom implementations
* <-> CSRF attacks
* `SecurityContext` object / `@AuthenticationPrincipal`