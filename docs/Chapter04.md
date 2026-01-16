# 4 스프링 시큐리티

1. 스프링 시큐리티 활성화하기
2. 스프링 시큐리티 구성하기
3. 웹 요청 보안 처리하기
4. 사용자 인지하기

(6th: 5장)

1. Enabling Spring Security
2. Configuring authentication
3. Securing Web requests
4. Applyling method-level security
5. Knowing your user

## 4.1 스프링 시큐리티 활성화하기

* [의존성 설정](/pom.xml)

* 구성 없이 첫 로드 시: 임시 계정 (user) 정보 logged
* 자동 제공되는 보안 구성
  * 모든 HTTP 요청 인증필요
  * 특수한 역할/권한 없음
  * 별도 로그인 페이지 없음
  * 스프링 시큐리티 기본 HTTP 인증을 통해 인증
  * 단일 사용자 (user)

* 이번 목표
  * 사용자 로그인 페이지
  * 다수 사용자 제공 / 신규 사용자 등록
  * 다른 HTTP 경로마다 다른 보안 규칙 적용

## 4.2 스프링 시큐리티 구성하기

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        .authorizeRequests()
            .antMatchers("/design", "/orders")
                .access("hasRole('ROLE_USER')")
            .antMatchers("/", "/**").access("permitAll")
        .and()
            .httpBasic();
    }


    // 인메모리 사용자 스토어에 사용자를 정의하는 경우
    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user1")
            .password("{noop}password1")
            .authorities("ROLE_USER")
            .and()
            .withUser("user2")
            .password("{noop}password2")
            .authorities("ROLE_USER");
    }
}
```

* 보안 관련 기능 테스트 시에는 브라우저의 InPrivate, Incognito 브라우징을 활용하는 것이 좋다

* 사용자 스토어 구성
  * 인메모리 사용자 스토어
  * JDBC 기반
  * LDAP 기반
  * 커스텀 사용자 명세

### 4.2.1 인메모리 사용자 스토어

* (미리 정해진) 사용자 정보 -> 앱이 점유한 메모리 내에 탑재
* 변동이 거의 없는 일부 사용자만이 앱에 접근하는 경우
* 사용자에 변동 사항이 생기는 경우 재구성, 재배포해야 함

```java
// 6th ed.
@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    List<UserDetails> usersList = new ArrayList<>();
    usersList.add(new User(
        "buzz", encoder.encode("password"),
        Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
    ));
    usersList.add(new User(
        "woody", encoder.encode("password"),
        Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))
    ));
    return new InMemoryUserDetailsManager(usersList);
}
```

### 4.2.2 JDBC 기반의 사용자 스토어

```java
@Autowired
DataSource dataSource;

@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .jdbcAuthentication()
        .dataSource(dataSource);
}
```

#### 스프링 시큐리티의 기본 사용자 쿼리를 대체하기

```java
// 기본 쿼리는 다음과 같이 작동한다
public static final String DEF_USERS_BY_USERNAME_QUERY = 
    "SELECT username, password, enabled " + 
    "FROM users " + 
    "WHERE username = ?"; 

public static final String DEF_AUTHORITIES_BY_USERNAME_QUERY = 
    "SELECT username, authority " + 
    "FROM authorities " + 
    "WHERE username = ?"; 

public static final String DEF_GROUP_AUTHORITIES_BY_USERNAME_QUERY = 
    "SELECT g.id, g.group_name, ga.authority " + 
    "FROM authorities g, group_members gm, group_authorities ga " + 
    "WHERE gm.username = ? " +
    "AND g.id = ga.group_id " +
    "AND g.id = gm.group_id"; 
```

* 적용 위해서는 다음과 같이 `/src/main/resources/schema.sql` 작성이 필요

```sql
-- schema.sql
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS ix_auth_username;

CREATE TABLE IF NOT EXISTS users(
  username VARCHAR2(50) NOT NULL PRIMARY KEY,
  password VARCHAR2(50) NOT NULL,
  enabled CHAR(1) DEFAULT '1'
);

CREATE TABLE IF NOT EXISTS authorities (
  username VARCHAR2(50) NOT NULL,
  authority VARCHAR2(50) NOT NULL,
  constraint fk_authorities_users FOREIGN KEY(username) REFERENCES users(username)
);

CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);


-- data.sql
INSERT INTO users (username, password) VALUES ('user1', 'password1');
INSERT INTO authorities (username, authority) VALUES ('user1', 'ROLE_USER');
COMMIT;
```

* PasswordEncoder 지정 반드시 필요 (Spring Security 5 ~ )

```java
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .jdbcAuthentication()
        .dataSource(dataSource)
        .usersByUsernameQuery(
            "SELECT username, password, enabled FROM users " +
            "WHERE useranme=?"
        )
        .authoritiesByUsernameQuery(
            "SELECT username, authority FROM authorities " +
            "WHERE username=?"
        );
}
```

* Spring Security 기본 SQL 쿼리 대체 시 규칙
  * (WHERE 절) 매개변수는 하나이며 반드시 username
  * 인증 시에는 username, password, enabled 값 반환
  * 권한 쿼리에서는 username과 authority를 포함하는 행들 반환
  * 그룹 권한 쿼리에서는 그룹 id, 그룹 이름, 권한 열의 행들 반환

#### 암호화된 비밀번호 사용하기

```java
// ...
        .authoritiesByUsernameQuery(
            "SELECT username, authority FROM authorities " +
            "WHERE username=?"
        )
        .passwordEncoder(new BCryptPasswordEncoder());
```

* Spring Security의 암호화 알고리즘 구현 클래스들
  * BCryptPasswordEncoder
  * NoOpPasswordEncoder
  * Pbkdf2PasswordEncoder
  * SCryptPasswordEncoder
  * StandardPasswordEncoder
  * 직접 구현
* Spring Security는 저장된 비밀번호를 해독하지 않는다
  * 항상 암호화하여 저장 / 입력받은 값을 암호화해서 비교

```java
// PasswordEncoder 인터페이스 정의
public interface PasswordEncoder {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}
```

### 4.2.3 LDAP 기반 사용자 스토어

* LDAP 기반 Spring Security 구성
  * `ldapAuthentication()`

```java
// LDAP의 루트부터 검색하는 경우
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        .userSearchFilter("(uid={0})")
        .groupSearchFilter("member={0}");
}

// 쿼리 기준점 제공하는 경우
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        .userSearchBase("ou=people")
        .userSearchFilter("(uid={0})")
        .groupSearchBase("ou=groups")
        .groupSearchFilter("member={0}");
}
```

#### 비밀번호 비교 구성하기

```java
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        .userSearchBase("ou=people")
        .userSearchFilter("(uid={0})")
        .groupSearchBase("ou=groups")
        .groupSearchFilter("member={0}")
        .passwordCompare();
}

// 비밀번호가 다른 속성에 지정되어 있는 경우
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        .userSearchBase("ou=people")
        .userSearchFilter("(uid={0})")
        .groupSearchBase("ou=groups")
        .groupSearchFilter("member={0}")
        .passwordCompare()
        .passwordEncoder(new BCryptPasswordEncoder())
        .passwordAttribute("userPasscode");
}
```

#### 원격 LDAP 서버 참조하기

```java
// 설정되지 않는 경우에는 localhost:33389로 간주함
@Override
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        .userSearchBase("ou=people")
        .userSearchFilter("(uid={0})")
        .groupSearchBase("ou=groups")
        .groupSearchFilter("member={0}")
        .passwordCompare()
        .passwordEncoder(new BCryptPasswordEncoder())
        .passwordAttribute("userPasscode");
        .contextSource().url("ldap://tacocloud.com:389:/dc=tacocloud,dc=com")
}
```

#### 내장된 LDAP 서버 구성하기

* Spring Security가 제공하는 내장 LDAP 서버 사용하기
  * 의존성 구성
    * `org.springframework.boot`: `spring-boot-starter-data-ldap`
    * `org.springframework.ldap`: `spring-ldap-core`
    * `org.springframework.security`: `spring-security-ldap`


```java
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        // ...
        .contextSource()
        .root("dc=tacocloud,dc=com");
}

// LDIF 파일을 찾게 하려면
// (/src/main/resources/users.ldif 제공되어야 함)
protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth
        .ldapAuthentication()
        // ...
        .contextSource()
        .root("dc=tacocloud,dc=com")
        .ldif("classpath:users.ldif")
        .and()
        .passwordCompare()
        // ...
        ;
}
```

### 4.2.4 사용자 인증의 커스터마이징

* 사용자 데이터를 JPA로...

#### 사용자 도메인 객체와 퍼시스턴스 정의하기

* [도메인 객체](/src/main/java/tacos/User.java)
* 참고: [UserDetails 문서](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details.html)
* [리퍼지터리 인터페이스](/src/main/java/tacos/data/UserRepository.java)

#### 사용자 명세 서비스 생성하기

```java
// Spring Security의 UserDetailsService 인터페이스
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```




## 요약

* Spring Security Auto-config
* 사용자 정보의 다양한 저장처
* CSRF
* `SecurityContext`, `@AuthenticationPrincipal`