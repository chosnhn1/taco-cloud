# 3 데이터로 작업하기

1. JDBC를 사용해서 데이터 읽고 쓰기
2. 스프링 데이터 JPA를 사용해서 데이터 저장하고 사용하기

* (예전에 했던 sbb와 많이 비교해보자 - 자동구현에 많이 의존)

## 3.1 JDBC를 사용해서 데이터 읽고 쓰기

* JdbcTemplate를 사용하지 않고 데이터 쿼리하는 예시

```java
@Override
public Ingredient findById(String id) {
    Connction connection = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
        connection = dataSource.getConnection();
        statement = connection.prepareStatement(
          "SELECT id, name, type FROM Ingredient WHERE id = ?"
        );
        statement.setString(1, id);
        resultSet = statement.executeQuery();
        Ingredient ingredient = null;
        if (resultSet.next()) {
            ingredient = new Ingredient(
                resultSet.getString("id"),
                resultSet.getString("name"),
                Ingredient.Type.valueOf(resultSet.getString("type")));
        }

        return ingredient;

    } catch (SQLException e) {
        // what can it be?
    } finally {
        // SQL 쿼리 처리 시에 발생할 수 있는 오류들을 핸들링하는 코드들
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {}
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {}
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {}
        }
    }

    return null;
}
```

* JdbcTemplate를 사용하는 예시

```java
private JdbcTemplate jdbc;

@Override
public Ingredient findById(String id) {
    return jdbc.queryForObject(
        "SELECT id, name, type FROM Ingredient WHERE id=?",
        this::mapRowToIngredient, id);
}

private Ingredient mapRowToIngredient(ResultSet rs, int rowNum) throws SQLException {
    return new Ingredient(
        rs.getString("id"),
        rs.getString("name"),
        Ingredient.Type.valueOf(rs.getString("type"));
    )
}
// 위 코드에 비하면? 아이고 편하다
```

### 3.1.1 퍼시스턴스를 고려한 도메인 객체 수정하기

* id와 생성시각 정보(유용함) 추가

### 3.1.2 JdbcTemplate 사용하기

* [리포지터리](/src/main/java/tacos/data/JdbcIngredientRepository.java)
* 사용 예시: [DesignTacoController](/src/main/java/tacos/web/DesignTacoController.java)

### 3.1.3 스키마 정의하고 데이터 추가하기

* ERD:
  * Taco_Order
    * id: identity
    * delivery...: varchar
    * cc...: varchar
    * placedAt: timestamp
  * Taco_Order_Tacos (M:N - maybe?)
    * tacoOrder: bigint
    * taco: bigint
  * Taco
    * id: identity
    * name: varchar
    * createdAt: timestamp
  * Taco_Ingredients (M:N)
    * taco: bigint
    * ingredient: varchar
  * Ingredient
    * id: varchar
    * name: varchar
    * type: varchar

* Spring Boot는 `/src/main/resources`에 저장된 `schema.sql`과 `data.sql`를 읽는다
* 작성된 [스키마](/src/main/resources/schema.sql)와 [데이터](/src/main/resources/data.sql)

### 3.1.4 타코와 주문 데이터 추가하기

* Ingredient 객체의 저장: 용이
* 복잡한 객체의 경우에는...?
* JdbcTemplate에서의 데이터 저장의 두 가지 방법
  * 직접 `update()`
  * `SimpleJdbcInsert` wrapper 사용

#### JdbcTemplate를 사용해서 데이터 저장하기

* 예시: [JdbcTacoRepository](/src/main/java/tacos/data/JdbcTacoRepository.java)
  * DesignTacoController에서 사용
#### SimpleJdbcInsert를 사용해서 데이터 추가하기

* 예시: [JdbcOrderRepository](/src/main/java/tacos/data/JdbcOrderRepository.java)
  * SimpleJdbcInsert와, 소비할 키값 쌍을 보내줄 ObjectMapper 사용
  * OrderController에 적용

* 형식 변환을 위한 Converter도 작성
  * 예시: [IngredientByIdConverter](/src/main/java/tacos/web/IngredientByIdConverter.java)

* (글쎄... `KeyHolder.getKey() is null`이 발생한다 - 왜일까)
* (해결 완료: 코드 참조)
* 6판은 전혀 다르게 구현한다 - 살펴봐야겠다

## 3.2 스프링 데이터 JPA를 사용해서 데이터 저장하고 사용하기

* Spring Data JPA
* Spring Data MongoDB
* Spring Data Neo4
* Spring Data Redis
* Spring Data Cassandra

### 3.2.1 스프링 데이터 JPA를 프로젝트에 추가하기

* [의존성 수정](/pom.xml)
* Spring Data JPA는 기본적으로 구현체인 Hibernate도 포함
  * 다른 걸 쓰고 싶다면 Hibernate를 제외하고 해당 라이브러리를 추가할 것 (e.g. EclipseLInk)

### 3.2.2 도메인 객체에 애너테이션 추가하기

* `@Entity`, `@Id`
* `@NoArgsConstructor` (JPA에서는 개체가 인자 없는 생성자를 가져야 함)

### 3.2.4 JPA 리퍼지터리 커스터마이징하기

```java
// 이게 필요하다고 하자
List<Order> findByDeliveryZip(String deliveryZip);
```

* Spring Data의 메서드 분석
* e.g. `readOrdersByDeliveryZipAndPlacedAtBetween()`:
  * 
* 커스텀 쿼리 w/ `@Query` annotationo 

```java
@Query("Order o where o.delivertyCity='Seattle'")
List<Order> readOrdersDeliveredInSeattle();
```
## 요약

* 용이한 JDBC 작업을 위한 JdbcTemplate
* DB의 ID값 접근 시: PrepareStatementCreator & KeyHolder
* SimpleJdbcInsert
* 스프링 데이터 JPA