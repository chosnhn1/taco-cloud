# 2 웹 애플리케이션 개발하기

1. 정보 보여주기
2. 폼 제출 처리하기
3. 폼 입력 유효성 검사하기
4. 뷰 컨트롤러로 작업하기
5. 뷰 템플릿 라이브러리 선택하기

## 2.1 정보 보여주기

### 2.1.1 도메인 설정하기

* [Ingredient](/src/main/java/tacos/Ingredient.java)
* [Taco](/src/main/java/tacos/Taco.java)

* Lombok
  * getter, setter, equals, hashCode, toString 등 작성 X
  * Lombok이 getter, setter 등 자동 작성

### 2.1.2 컨트롤러 클래스 생성하기

* [DesignTacoController](/src/main/java/tacos/web/DesignTacoController.java)

* Slf4j Logger

```java
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DesignTacoController.class);
// ==

@Slf4j
// ...
```

#### GET 요청 처리하기

* `@GetMapping`: introduced in Spring 4.3
  * 그 전에는 `@RequestMapping(method=RequestMethod.GET)`
  * 특화된 애너테이션 사용하기

### 2.1.3 뷰 디자인하기

* [design.html](/src/main/resources/templates/design.html)
* [CSS](/src/main/resources/templates/style.css)

## 2.2 폼 제출 처리하기

* [orderForm.html](/src/main/resources/templates/orderForm.html)
* [OrderController](/src/main/java/tacos/web/OrderController.java)
* [Order](/src/main/java/tacos/Order.java)

## 2.3 폼 입력 유효성 검사하기

* (변화점: javax -> build Spring Validation Starter)
* (`jakarta.validation` package)

* 수동 조건문 (meh...)
* Bean Validation API & Hibernate Component (Bean Validation API의 구현체)
* 절차
  1. 대상 클래스에 검사 규칙 선언
  2. 검사가 이뤄져야 하는 메서드에 수행 여부 지정
  3. 에러를 표시하도록 폼 뷰 수정

* 참고: `org.hibernate.validator.constraints.CreditCardNumber` annotation
  * Luhn Algorithm test 수행

### 2.3.1 규칙 선언

* -> Taco, Order Class

### 2.3.2 폼과 바인딩될 때 유효성 검사 수행하기

* -> processDesign method
* `processDesign(Taco taco)` -> `processDesign(@Valid Taco taco, Errors errors)`

### 2.3.3 유효성 검사 에러 보여주기

* Thymeleaf의 `fields`, `th:errors` 속성

## 2.4 뷰 컨트롤러로 작업하기

* 일반적인 컨트롤러 패턴
  1. `@Controller` annotation으로 컴포넌트 검색 / 빈 생성 컨트롤러 클래스 표시
  2. 요청 패턴 표시 위한 `@RequestMapping` annotation
  3. 종류별 요청 처리: `@GetMapping` 등 메서드별 애너테이션
* `HomeController`처럼 모델 데이터나 사용자 입력을 처리하지 않는 간단한 컨트롤러: 뷰 컨트롤러 사용 가능

* 뷰 컨트롤러 선언: [예시](/src/main/java/tacos/web/WebConfig.java)

* 참고: 새 구성 클래스를 별도로 선언하지 않고 다른 구성 클래스에서 오버라이딩하는 예시:
  * 분리 여부는 개발자 선택
```java
@SpringBootApplication
public class TacoCloudApplication implements WebMvcConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(TacoCloudApplication.class, args);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
    }
}
```

## 2.5 뷰 템플릿 라이브러리 선택하기

* Spring Boot 자동구성 지원
  * FreeMarker
  * Groovy Template
  * JavaServer Pages (JSP)
  * Mustache
  * Thymeleaf


* Mustache 사용 예시

```html
<!-- 어디서 많이 본 것 같다... -->
<h3>Designate your wrap:</h3>
{{#wrap}}
<div>
  <input name="ingredients" type="checkbox" value="{{id}}" />
  <span>{{name}}</span><br/>
</div>
{{/wrap}}
```

* JSP: Servlet 컨테이너가 이미 JSP 명세 구현하므로 따로 의존성 지정할 필요 없음
  * 단, /web-inf 밑에서 JSP 코드를 찾으므로 JAR 대신 WAR 사용해야...

### 2.5.1 템플릿 캐싱

* in Production: 성능 향상
* in Development: 불편 (애플리케이션 다시 시작해야 한다니!)
* 캐싱 비활성화 (application.properties 에서 설정)
  * FreeMaker: `spring.freemaker.cache`
  * Groovy Template: `spring.groovy.template.cache`
  * Mustache: `spring.mustache.cache`
  * Thymeleaf: `spring.thymeleaf.cache`
* (Spring Devtools를 사용한다면? 알아서 해준다)

## 요약

* 스프링 MVC: 스프링 앱의 웹 프런트엔드
  * Annotation 기반 요청 처리
* 요청 처리 메서드는 보통 논리 뷰 이름을 반환
* 스프링 MVC의 유효성 검사: Java Bean Validation API / Hibernate Validator ...
* 모델 데이터가 없는 요청 등: 뷰 컨트롤러
* 스프링 뷰 템플릿: Thymeleaf 등...