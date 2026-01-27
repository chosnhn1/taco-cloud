# 7 REST 서비스 사용하기

* Spring Application이 (소비자로서) 다른 REST API와 상호작용하는 방법들
  * RestTemplate
  * Traverson
  * WebClient

1. RestTemplate으로 REST 엔드포인트 사용하기
  1. 리소스 가져오기
  2. 리소스 쓰기
  3. 리소스 삭제하기
  4. 리소스 데이터 추가하기
2. Traverson으로 REST API 사용하기
3. Application 빌드 및 실행하기

* (좀 가벼운 내용이다...)

## 7.1 RestTemplate으로 REST 엔드포인트 사용하기

```java
// 필요할 때 인스턴스 생성
RestTemplate rest = new RestTemplate();

// 혹은 빈으로 주입
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

### 7.1.1

```java
public Ingredient getIngredientById(String ingredientId) {
    return rest.getForObject("http://localhost:8080/ingredients/{id}", Ingredient.class, ingredientId);
}

// 또는
public Ingredient getIngredientById(String ingredientId) {
    Map<String, String> urlVariables = new HashMap<>();
    urlVariables.put("id", ingredientId);
    return rest.getForObject("http://localhost:8080/ingredients/{id}", Ingredient.class, urlVariables);

// 또는
public Ingredient getIngredientById(String ingredientId) {
    Map<String, String> urlVariables = new HashMap<>();
    urlVariables.put("id", ingredientId);
    URI url = UriComponentsBuilder
        .fromHttpUrl("http://localhost:8080/ingredients/{id}")
        .build(urlVariables);

    return rest.getForObject(url, Ingredient.class);
}
```

* 응답 도메인 객체 대신, 그걸 포함하는 ResponseEntity로 반환하는 `getForEntity()`

```java
public Ingredient getIngredientById(String ingredientId) {
    ResponseEntity<Ingredient> responseEntity = rest.getForEntity("http://localhost:8080/ingredients/{id}", Ingredient.class, ingredientId);
    log.info("Fetched time: " + responseEntity.getHeaders().getDate());

    return responseEntity.getBody();
}
```

### 7.1.2

```java
public void updateIngredient(Ingredient ingredient) {
    rest.put("http://localhost:8080/ingredients/{id}", ingredient, ingredient.getId());
}
```

### 7.1.3

```java
public void deleteIngredient(Ingredient ingredient) {
    rest.delete("http://localhost:8080/ingredients/{id}", ingredient.getId());
}
```

### 7.1.4

```java
public Ingredient createIngredient(Ingredient ingredient) {
    return rest.postForObject("http://localhost:8080/ingredients", ingredient, Ingredient.class);
}

// 새 리소스 위치 필요한 경우
public Ingredient createIngredient(Ingredient ingredient) {
    return rest.postForLocation("http://localhost:8080/ingredients", ingredient);
}

// 위치와 객체 다 필요한 경우
// -> postForEntity
public Ingredient createIngredient(Ingredient ingredient) {
    ResponseEntity<Ingredient> responseEntity = rest.postForEntity("http://localhost:8080/ingredients", ingredient, Ingredient.class);

    log.info("New resource created at " + responseEntity.getHeaders().getLocation());
    return responseEntity.getBody();
}
```

## 7.2 Traverson으로 REST API 사용하기