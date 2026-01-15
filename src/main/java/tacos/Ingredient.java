package tacos;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// JDBC
// @Data
// @RequiredArgsConstructor
// public class Ingredient {
    
//     private final String id;
//     private final String name;
//     private final Type type;

//     public static enum Type {
//         WRAP, PROTEIN, VEGGIES, CHEESE, SAUCE
//     }
// }

// JPA
@Data
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@Entity
public class Ingredient {
    
    @Id
    private final String id;
    private final String name;
    private final Type type;

    public static enum Type {
        WRAP, PROTEIN, VEGGIES, CHEESE, SAUCE
    }
}
