# 6 REST 서비스 생성하기

* Web has changed...
* 다양한 사용처를 위한 REST API

## 6.1 REST 컨트롤러 작성하기

* 지금까지 사용한 Thymeleaf
* 하지만 SPA는?
  * (React는? Vue는? Angular는? ...)
* 그러면 백엔드 서버는 어떻게 구성되어야 할까?

* (앵귤러) 클라이언트 <-> HTTP Request <-> Spring MVC가 제공하는 REST API

* Spring MVC의 HTTP Req-Res Annotations
  * @GetMapping
  * @PostMapping
  * @PutMapping
  * @PatchMapping
  * @DeleteMapping
  * @RequestMapping: 다목적

### 6.1.1 서버에서 데이터 가져오기

* [DesignTacoController](/src/main/java/tacos/web/api/DesignTacoController.java)

### 6.1.2 서버에 데이터 전송하기

```java
@PostMapping(consumes="application/json")
@ResponseStatus(HttpStatus.CREATED)
public Taco postTaco(@RequestBody Taco taco) {
    return tacoRepo.save(taco);
}
```

* `consumes`: Content-type가 해당 내용과 일치하는 것만 처리
* @RequestBody: JSON 데이터를 taco param과 bind
  * 미지정하면 쿼리, 폼 등이 곧바로 Taco와 bind한다고 간주
* postTaco: Taco -> save()
* @ResponseStatus(HttpStatus.CREATED): 결과 201

### 6.1.3 서버의 데이터 변경하기

* PUT
  * GET의 반대 의미
  * 데이터 전체의 교체
* PATCH
  * 데이터의 일부분 변경

```java
// 전체 교체
@PutMapping("/{orderId}")
public Order putOrder(@RequestBody Order order) {
    return repo.save(order);
}
```

```java
@PatchMapping(path="/{orderId}", consumes="application/json")
public Order patchOrder(@PathVariable("orderId") Long orderId, @RequestBody Order patch) {

    Order order = repo.findById(orderId).get();
    // 데이터 일부 변경의 로직:
    if (patch.getDeliveryName() != null) {
        order.setDeliveryName(patch.getDeliveryName());
    }
    if (patch.getDeliveryStreet() != null) {
        order.setDeliveryStreet(patch.getDeliveryStreet());
    }
    // ...

    return repo.save(order);
}
```

* 주의: PATCH의 여러 방법
  * 지금 사용하는 방법의 문제
  * "변경하지 않음"의 의미로 null을 쓴다면, 실제 null 값이 변경할 값이라면 어떻게 해야 하는가
  * 컬렉션 항목의 삭제, 추가는 어떻게 하는가

### 6.1.4 서버에서 데이터 삭제하기

```java
@DeleteMapping("/{orderId}")
@ResponseStatus(code=HttpStatus.NO_CONTENT)
public void deleteOrder(@PathVariable("orderId") Long orderId) {
    try {
        repo.deleteById(orderId);
    } catch (EmptyResultDataAccessException e) {}     // <- 없는 주문을 삭제: 할 것이 없다
                                                      // 물론 404 처리를 할 수도 있다
}
```

## 6.2 하이퍼미디어 사용하기

* API URL Scheme을 어떻게 알지...?
* HATEOAS
  * "Hypermedia as the Engine of Application State"
  * API가 반환하는 리소스에 관련 하이퍼링크를 포함
    * 이 경우 최소한의 API URL로 다른 URL을 알 수 있다

  * API + Hypermedia
  * JSON 응답에 하이퍼링크 포함: HAL (Hypertext Application Language)

* 적용: `spring-boot-starter-hateoas` 의존성

* 참고: HATEOAS와 OpenAPI
  * (나는 Swagger가 친숙하다... 오늘날은 어떤가)
  * []
  * [OpenAPI Intiative](https://www.openapis.org/)

### 6.2.1 하이퍼링크 추가하기

* Spring HATEOAS 사용 기본 타입
  * `Resource`
  * `Resources`

```java
@GetMapping("/recent")
public Resources<Resource<Taco>> recentTacos() {
    PageRequest page = pageRequest.of(
        0, 12, Sort.by("createdAt").descending());
    
    List<Taco> tacos = tacoRepo.findAll(page).getContent();
    Resources<Resource<Taco>> recentResources = Resources.wrap(tacos);
    recentResources.add(new Link("http://localhost:8080/design/recent", "recents"));    // <- hard-coded
    return recentResources;
}
```

```json
// <tacos 응답에 아래 링크가 추가될 것>
"_links": {
  "recents": {
    "href": "http://localhost:8080/design/recent"
  }
}
```

* 하드코딩하지 않는 방법...
* `ControllerLinkBuilder`

```java
Resources<Resource<Taco>> recentResources = Resources.wrap(tacos);
recentResources.add(
    ControllerLinkBuilder.linkTo(DesignTacoController.class)    // 컨트롤러의 기본 경로를 사용
        .slash("recent")        // 나머지 "/recent" 경로
        .withRel("recents"));   // 관계 이름
                                // 편안
```

* `slash()` 대신 `linkTo()` 사용

```java
Resources<Resource<Taco>> recentResources = Resources.wrap(tacos);
recentResources.add(
    ControllerLinkBuilder.linkTo(methodOn(DesignTacoController.class).recentTacos())
        .withRel("recents"));

```

### 6.2.2 리소스 어셈블러 사용하기

* 타코 리소스마다의 링크는 어떻게 추가할까
* Link 루프? 번거롭다
* 유틸리티 클래스 `TacoResource` 작성: ResourceSupport를 상속해서 쓰자

* (HATEOAS를 적용해주는 serializer를 보는 것 같다)

```java

public class TacoResource extends ResourceSupport {
    @Getter
    private final String name;

    @Getter
    private final Date createdAt;

    @Getter
    private final List<Ingredient> ingredients;

    public TacoResource(Taco taco) {
        this.name = taco.getName();
        this.createdAt = taco.getCreatedAt();
        this.ingredients = taco.getIngredients()
    }
}
```

```java
public class TacoResourceAssembler() extends ResourceAssemblerSupport<Taco, TacoResource> {
    super(DesignTacoController.class, TacoResource.class);
}

@Override
protected TacoResource instantiateResource(Taco taco) {
    return new TacoResource(taco);
}

@Override
public TacoResource toResource(Taco taco) {
    return createResourceWithId(taco.getId(), taco);
}
```

* 만든 유틸리티 클래스의 사용법

```java
@GetMapping("/recent")
public Resources<TacoResource> recentTacos() {
    PageRequest page = pageRequest.of(
        0, 12, Sort.by("createdAt").descending());
    
    List<Taco> tacos = tacoRepo.findAll(page).getContent();

    // 여기서부터
    // Resources<Resource<Taco>> recentResources = Resources.wrap(tacos);
    List<TacoResource> tacoResources = new TacoResourceAssembler().toResources(tacos);
    Resources<TacoResource> recentResources = new Resources<TacoResource>(tacoResources);
    // 여기까지 주목

    recentResources.add(
    ControllerLinkBuilder.linkTo(methodOn(DesignTacoController.class).recentTacos())
        .withRel("recents"));

    return recentResources;
}
```

* 하위 Ingredient 들에도 적용하고 싶다

### 6.2.3 embedded 관계 이름 짓기

## 6.3 데이터 기반 서비스 활성화하기

* Spring Data REST
  * Spring Data가 생성하는 Repository의 REST API를 자동 생성
    * (이런거 좀 빨리 알려줘요)
    * (물론 나라면 직접 작성하겠지만)
  * 자동구성 사용
    * 기존에 작성한 컨트롤러와 충돌하지 않도록 `spring.data.rest.base-path` 속성 설정할 것

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

### 6.3.2 페이징과 정렬

### 6.3.3 커스텀 엔드포인트 추가하기

### 6.3.4 커스텀 하이퍼링크를 스프링 데이터 엔드포인트에 추가하기

## 6.4 앵귤러 IDE 이클립스 플러그인 설치와 프로젝트 빌드 및 실행하기

* (생략: 나는 VSCode 쓴다)
* (또 나는 Angular보다 React다)
* 한번 읽어볼 것

