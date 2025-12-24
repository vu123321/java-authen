package com.vule.authen.controller;

import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.IngredientResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.service.IngredientService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngredientController {
    IngredientService ingredientService;

    @GetMapping
    public ApiResponse<PageResponse<IngredientResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.<PageResponse<IngredientResponse>>builder()
                .code(1000)
                .message("Success")
                .result(ingredientService.list(keyword, page, sort))
                .build();
    }
}
