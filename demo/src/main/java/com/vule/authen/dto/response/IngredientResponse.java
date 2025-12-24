package com.vule.authen.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class IngredientResponse {
    private String id;
    private String code;
    private String name;
    private BigDecimal price;
    private Integer number;
}

