package tacos.data;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import tacos.Ingredient;
import tacos.Taco;

@Repository
public class JdbcTacoRepository implements TacoRepository {
// TacoRepository를 구현해보자

    private JdbcTemplate jdbc;

    public JdbcTacoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    // Taco save
    // Ingredient(ID가 이미 주어진 String)와의 차이에 주목: DB가 생성한 id를 받아와서 Ingredient-Taco 참조 관계도 저장해야 한다
    @Override
    public Taco save(Taco taco) {
        long tacoId = saveTacoInfo(taco);
        taco.setId(tacoId);
        // Taco가 가진 Ingredient를 순회하며 저장하기
        for (Ingredient ingredient : taco.getIngredients()) {
            saveIngredientToTaco(ingredient, tacoId);
        }

        // 완전한 taco를 돌려주자
        return taco;
    }

    // 새 Taco를 DB에 저장하고, 해당 id를 KeyHolder를 거쳐 반환받는 프로세스
    private long saveTacoInfo(Taco taco) {

        // 현재 날짜로 생성 날짜
        taco.setCreatedAt(new Date());

        // ! SiA 5th: Factory의 returnGeneratedKeys가 false이므로 true로 바꿔주어야 함
        PreparedStatementCreatorFactory pscf = new PreparedStatementCreatorFactory(
            "INSERT INTO Taco (name, createdAt) VALUES (?, ?)",
            Types.VARCHAR, Types.TIMESTAMP
        );
        pscf.setReturnGeneratedKeys(true);


        PreparedStatementCreator psc = pscf
        .newPreparedStatementCreator(
            Arrays.asList(
                taco.getName(),
                new Timestamp(taco.getCreatedAt().getTime())));


        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(psc, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void saveIngredientToTaco(Ingredient ingredient, long tacoId) {
        jdbc.update("INSERT INTO Taco_ingredients (taco, ingredient) " + "VALUES (?, ?)", tacoId, ingredient.getId());
    }


}
