package tacos.data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import tacos.Order;
import tacos.Taco;
import tools.jackson.databind.ObjectMapper;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    // 여기서는 SimpleJdbcInsert 사용법을 익히자
    private SimpleJdbcInsert orderInserter;
    private SimpleJdbcInsert orderTacoInserter;
    private ObjectMapper objectMapper;

    @Autowired
    public JdbcOrderRepository(JdbcTemplate jdbc) {
        this.orderInserter = new SimpleJdbcInsert(jdbc)
            .withTableName("Taco_Order")
            .usingGeneratedKeyColumns("id");
        
        this.orderTacoInserter = new SimpleJdbcInsert(jdbc)
            .withTableName("Taco_Order_Tacos");

        this.objectMapper = new ObjectMapper();
    }

    // 저장 메서드이지만, 세부 저장은 하위 메서드에 위임한다
    public Order save(Order order) {
        order.setPlacedAt(new Date());
        long orderId = saveOrderDetails(order);
        order.setId(orderId);
        List<Taco> tacos = order.getTacos();

        for (Taco taco : tacos) {
            saveTacoToOrder(taco, orderId);
        }

        return order;
    }

    // 주문 세부사항을 저장
    private long saveOrderDetails(Order order) {
        // Inserter가 소비할 키값쌍(Map)을 만들어주자
        // 키는 열 이름, 값은 해당 열에 추가될 값

        // 여기서는 Jackson(JAVA JSON 라이브러리) ObjectMapper를 활용해 키값쌍을 생성하는 예시
        // 제공할 Map만 적절히 구성할 수 있다면 어떤 방법을 택해도 좋다
        @SuppressWarnings("unchecked")
        Map<String, Object> values = objectMapper.convertValue(order, Map.class);
        values.put("placedAt", order.getPlacedAt());

        long orderId = orderInserter
            .executeAndReturnKey(values)
            .longValue();

        return orderId;
    }

    // 주문과 Taco 관계 저장
    private void saveTacoToOrder(Taco taco, long orderId) {
        Map<String, Object> values = new HashMap<>();
        values.put("tacoOrder", orderId);
        values.put("taco", taco.getId());
        orderTacoInserter.execute(values);
    }


}
