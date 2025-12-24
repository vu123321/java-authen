package com.vule.authen.dto.request;
import lombok.*;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String ingredientCode;
        private Integer number;
    }
}
