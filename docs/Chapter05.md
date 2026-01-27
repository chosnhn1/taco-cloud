# 5 구성 속성 사용하기

* Spring Boot Autoconfiguration
* Configuration Property
* JVM System props, command-line props, OS env, ...

1. 자동-구성 세부 조정하기
  1. 스프링 환경 추상화 이해하기
  2. 데이터 소스 구성하기
  3. 내장 서버 구성하기
  4. 로깅 구성하기
2. 우리의 구성 속성 생성하기
  1. 구성 속성 홀더 정의하기
  2. 구성 속성 메타데이터 선언하기
3. 프로파일 사용해서 구성하기
  1. 프로파일 특정 속성 정의하기
  2. 프로파일 활성화하기
  3. 프로파일을 사용해서 조건별로 빈 생성하기

(Python Django의 settings.py를 생각하면 어떨까)

## 5.1. 자동-구성 세부 조정하기

* 스프링의 두 가지 구성
  * 빈 연결 (Bean wiring): SAC Bean으로 생성되는 app component, 상호간 주입 방법 선언
  * 속성 주입 (Property injection): SAC에서 빈 속성값 설정

```java
// 내장 H2 DB를 DataSource로 선언:
@Bean
public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("schema.sql")
        .addScripts("user_data.sql", "ingredient_data.sql")
        .build();
}

// 하지만, 굳이 위처럼 하지 않아도 H2 라이브러리를 쓴다면 자동으로 classpth에서 해당 빈을 찾아 설정한다
// 그런데, 기본 읽어들이는 SQL 스크립트 등을 다르게 쓰고 싶다던가 한다면?
```

### 5.1.1 스프링 환경 추상화 이해하기

* Spring의 "environment abstraction"
  * 구성 가능한 모든 속성을 *한 곳에서* 관리
  * 스프링이 원천 속성을 가져오는 속성의 근원들
    * JVM 시스템 속성
    * OS 환경변수
    * 명령행 인자
    * App의 속성 구성 파일

* (원천속성들) -> 스프링 환경 -> 각종 빈

* e.g. Servlet container의 포트 변경

```
(application.properties)
server.port=9090
```

  * 혹은,

```yaml
# application.yaml
server:
  port: 9090
```

  * 혹은,

```sh
export SERVER_PORT=9090
```

* (14장: 구성 서버)
* Spring의 수백가지 사용가능한 구성속성들

### 5.1.2 데이터 소스 구성하기

* App 데이터 소스 - 현재 H2
  * 하지만, production에서는 다른 것을 써야 할 것
* DataSource 빈 명시적 구성: 가능함
* 스프링 부트에서의 구성 속성 사용: 편리함
* e.g. MySQL을 쓴다면

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost/tacocloud
    username: tacodb
    password: tacopassword
    driver-class-name: com.mysql.jdbc.Driver  # <- 스프링 부트가 JDBC 드라이버 클래스를 알아서 찾아주지만, 직접 설정할 수도 있다
```

* Spring Boot가 찾는 커넥션 풀
  * Tomcat JDBC
  * HikariCP
  * Commons DBCP 2
* 구성을 이용하면 그밖에 어떤 것도 쓸 수 있다

* 읽어들일 초기화 SQL 스크립트?
* 이렇게 간단히 설정 가능:

```yaml
spring:
  datasource:
    schema:
      - order-schema.sql
      - ingredient-schema.sql
      - taco-schema.sql
      - user-schema.sql
    data:
      - ingredients.sql
```

* JNDI 구성도 가능

```yaml
spring:
  datasource:
    jndi-name: java:/comp/env/jdbc/tacoCloudDS  # 단 이 설정 시 다른 datasource 구성 무시
```

### 5.1.3 내장 서버 구성하기

* 포트 번호

```yaml
server:
  port: 0   # 이러면? 서버 포트가 0이 되는게 아니라, 사용 가능한 포트를 무작위 선택함
            # 충돌 회피 (자동화 통합 테스트), 마이크로서비스 등에서 사용
```

* HTTPS 요청 처리 컨테이너 설정

```sh
# 키스토어 생성
keytool -keystore mykeys.jks -genkey -alias tomcat -keyalg RSA
```

```yaml
# application.yaml
# 내장 서버 HTTPS 활성화를 위한 속성값들
# 명령행 (매우 귀찮음) 대신 이렇게 설정할 수 있다
server:
  port: 8443
  ssl:
    key-store: file:///path/to/mykeys.jks       # JAR에 넣는다면 classpath: 가 URL이 될 것
    key-store-password: letmein
    key-password: letmein
```

### 5.1.4 로깅 구성하기

* Logging
* Spring Boot의 기본 로깅: INFO level로 Logback을 통해 구성
* 로깅 구성 제어는 classpath의 루트(`src/main/resources`)에서 logback.xml 파일로
  * 아래: 기본 로깅 구성과 동일한 예
* 스프링 부트를 쓴다면 아래 파일을 직접 작성하지 않고도 변경할 수 있다

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>
        %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
      </pattern>
    </encoder>
  </appender>
  <logger name="root" level="INFO"/>
  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

* 스프링 부트를 사용한 로거
  * e.g. 루트는 WARN으로, Security는 DEBUG 수준으로

```yaml
logging:
  level:
    root: WARN
    org:
      springframework:
        security: DEBUG
```

```yaml
logging:
  level:
    root: WARN
    org.springframework.security: DEBUG   # 가독성을 위해 붙여쓴 예
```

  * 로그 파일 위치

```yaml
logging:
  path: /var/logs/
  file: TacoCloud.log
  level:
    root: WARN
    org:
      springframework:
        security: DEBUG
```

### 5.1.5 다른 속성의 값 가져오기

* 꼭 String 하드 코딩일 필요도 없다

```yaml
greeting:
  welcome: ${spring.application.name}   # 다른 속성으로부터 한 속성의 값 설정
```

```yaml
greeting:
  welcome: You are using ${spring.application.name}.   # 다른 텍스트 속에 포함시키기
```

## 5.2. 우리의 구성 속성 생성하기

* 구성 속성을 주입하는 `@ConfigurationProperties` 애너테이션
  * 빈 속성을 스프링 환경에서 주입할 수 있다

* e.g. 사용자 주문을 저장하고 보여주기

```java
// OrderController
@GetMapping
public String ordersForUser(
    @AuthenticationPrincipal User user, Model model) {
        model.addAttribute("orders", orderRepo.findByUserOrderByPlacedAtDesc(user));

        return "orderList";
    }

// OrderRepository
    List<Order> findByUserOrderByPlacedAtDesc(User user);
```

* 그런데, 주문 수가 많아서 최근 주문만 보여주고 싶다면

```java
// OrderController
@GetMapping
public String ordersForUser(
    @AuthenticationPrincipal User user, Model model) {

        Pageable pageable = PageRequest.of(0, 20);    // <-
        model.addAttribute("orders", orderRepo.findByUserOrderByPlacedAtDesc(user, pageable));    // <-

        return "orderList";
    }

// OrderRepository
    List<Order> findByUserOrderByPlacedAtDesc(User user, Pageable pageable);
```

* 그런데, 페이지 크기를 하드코딩하니 찜찜하다
* 페이지 크기를 커스텀 구성 속성으로 설정하면 어떨까

```java

@Controller
// ...
@ConfigurationProperties(prefix="taco.orders")      // <- 구성 속성을 쓰도록 설정
public class OrderController {

    private int pageSize = 20;

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    // ...

    @GetMapping
    public String ordersForUser(
      // ...
    ) {
        Pageable pageable = PageRequest.of(0, pageSize);      // <- 하드코딩 대신 변수를 사용하도록 설정
        // ...
    }

}
```

```yaml
# application.yaml
taco:
  orders:           # <- 사용한 prefix와 일치하게
    pageSize: 10    # 이제 page는 10개다
```

* 프로덕션 중에 급하게 바꿔야 하면?

```sh
export TACO_ORDERS_PAGESIZE=10
```

### 5.2.1 구성 속성 홀더 정의하기

* 구성 속성을 꼭 컨트롤러, 특정 빈에만 쓸 필요 없다: '데이터 홀더 빈'에 지정하기
  * 구성 관련 정보를 별도로 유지, 여러 빈이 공통으로 쓰는 구성 속성을 쉽게 공유
  * e.g. `pageSize` 속성을 홀더 클래스 `OrderProps`가 추출하도록 하자

```java
// OrderProps Class
@Component
@ConfigurationProperties(prefix="taco.orders")
@Data
public class OrderProps {
    
    private int pageSize = 20;    // 앞에서 OrderController에서 하던 것과 비슷

}
```

```java
//OrderController
@Controller
@RequestMapping("/orders")
@SessionAttributes("order")   // <- 이제 ConfProp 필요없다
public class OrderController() {
    
    // ...
    private OrderProps props;         // <- OrderProps 빈 주입 (이거 써야지)

    public OrderController(OrderRepository orderRepo, OrderProps props) {   // <- 컨트롤러가 들고 오는 식
        this.props = props;
    }

    @GetMapping
    public String ordersForUser(
      // ...
    ) {
        Pageable pageable = PageRequest.of(0, props.getPageSize())    // <- props에서 찾아온다
    }
}
```

* 속성의 추가, 삭제, 이름 변경 등을 사용처와 분리
  * 이것들은 홀더가 할 일
  * e.g. 설정값의 유효성을 검사하고 싶다면:

```java
//OrderProps

@Component
@ConfigurationProperties(prefix = "taco.orders")
// ...
@Validated
public class OrderProps {

    @Min(value=5, message="must be between 5 and 25")
    @Max(value=25, message="must be between 5 and 25")
    private int pageSize = 20;
}
```

### 5.2.2 구성 속성 메타데이터 선언하기

* IDE에서 직접 만든 구성 속성을 잘 못 알아먹는 경우: 메타데이터가 필요하다
* 동작 필요사항은 아니지만 유용하다

* `spring-boot-configuration-processor` 사용
* `/src/main/resources/META-INF/`에 JSON 파일로 작성

```json
{
  "properties": [
    {
      "name": "taco.orders.page-size",
      "type": "int",
      "description": "Sets the maximum number of orders to display in a list"
    }
  ]
}
```

## 5.3. 프로파일 사용해서 구성하기

* 많은 경우 구성 명세가 환경에 따라 갈린다
  * 개발중? 테스트중? 실제 서비스중? ...
  * 이 때는 `application.properties`, `application.yml`보다 운영체제 환경변수를 사용하는 게 방법이다
  * 하지만 번거롭고, 관리하기도 디버그하기도 까다롭다

```sh
# 예를 들면... OS 환경변수에 꽂아주는 데이터소스
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost/tacocloud
export SPRING_DATASOURCE_USERNAME=tacouser
export SPRING_DATASOURCE_PASSWORD=tacopassword
```

* 대안: 스프링 프로파일
  * 프로파일을 "활성화" -> 어떤 구성이 적용되고 무시될지 결정

### 5.3.1 프로파일 특정 속성 정의하기

* 특정 속성을 정의하는 파일
  * 명명 규칙:
    * `application-[프로파일 이름].yml`
    * `application-[프로파일 이름].properties`
  
```yml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost/tacocloud
    username: tacouser
    password: tacopassword
logging:
  level:
    tacos: WARN
```

* `application.yml`을 내부에서 가르는 방법도 있음 (properties는 안 됨)
  * `---` 하고 `spring.profiles` (프로필 이름) 지정

```yml
logging:
  level:
    tacos: DEBUG

---
spring:
  profiles: prod
  datasource:
    url: #...

logging:
  level:
    tacos: WARN
```

### 5.3.2 프로파일 활성화하기

* 위에서 작성한 프로파일의 사용법
  1. spring.profiles.active에 지정 (예?)
  2. 환경 변수 사용

```sh
# OS 환경변수
export SPRING_PROFILES_ACTIVE=prod

# JAR 명령행 인자
java -jar taco-cloud.jar --spring.profiles.active=prod

# 여러개도 된다
export SPRING_PROFILES_ACTIVE=prod,audit,ha
```

```yml
spring:
  profiles:
  active:
    - prod
    - audit
    - ha
```

### 5.3.3 프로파일을 사용해서 조건별로 빈 생성하기

* 빈은 기본적으로 프로파일과 무관하게 생성
* 특정 프로파일이 활성화되어야만 생성되도록 하려면?
* `@Profile`
  * e.g. TacoCloudApplication: CommandLineRunner 빈
    * 실제 서비스 시에는 필요없다

```java
@Bean
@Profile("dev")   // <- dev 프로파일
// 혹은
@Profile({"dev", "qa"})   // <- dev or qa 프로파일
// 혹은
@Profile("!prod")   // <- prod 프로파일 비활성화인 경우 생성
public CommandLineRunner dataLoader(IngredientRepository repo, UserRepository userRepo, PasswordEncoder encoder) {
    // ...
}
```

* @Configuration 전체에 적용하는 예
  * 그 안에 적용된 Bean들이 해당

```java
@Profile({"!prod", "!qa"})
@Configuration
public class DevelopmentConfig {

    @Bean
    public // ...
}
```

## 요약

* `@ConfigurationProperties`로 구성 속성값 주입
* 구성 속성은 어디서?
  * 명령행 인자
  * OS 환경변수
  * JVM 시스템 속성
  * properties, yml 파일
  * 커스텀 속성
* 스프링의 자동 구성을 변경할 수도 있다
* 프로파일!

* (6판도 대동소이하다: 천천히 참고)
