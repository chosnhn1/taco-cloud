# 8 비동기 메시지 전송하기

* App 간 모든 메시징이 동기일 필요는 없다
  * 비동기 메시지 (던지고 간다)

1. JMS로 메시지 전송하기
2. RabbitMQ와 AMQP 사용하기
3. 카프카 사용하기
4. App 빌드 및 실행

* Java Massage Service (JMS)
* [RabbitMQ](https://www.rabbitmq.com/)
  * AMQP (Advanced Message Queueing Protocol 구현체)
* [Apache Kafka](https://kafka.apache.org/)

* 스프링 메시지 기반 POJO(Plain Old Java Object) 지원

## 8.1 JMS로 메시지 전송하기

* 2001년 도입된 Java 비동기 메시지 처리 표준
* 스프링: JmsTemplate
  * producer -> msg to queue/topic -> consumer

### 8.1.1 JMS 설정하기

* 스타터 의존성 설정
  * 메시지 브로커
    * Apache ActiveMQ
    * Apache ActiveMQ Artemis
  
* Artemis 브로커의 사용 속성
  * 기본 설정: localhost:61616
  * `spring.artemis.host`
  * `spring.artemis.port`
  * `spring.artemis.user`
  * `spring.artemis.password`

```yml
spring:
  artemis:
    host: artemis.tacocloud.com
    port: 61617
    user: tacoweb
    password: 13tm31n
```

* (참고: 이전 ActiveMQ의 경우)
  * `spring.activemq.broker-url`
  * `spring.activemq.user`
  * `spring.activemq.password`
  * `spring.activemq.in-memory`

