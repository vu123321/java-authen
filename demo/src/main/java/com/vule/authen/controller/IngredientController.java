package com.vule.authen.controller;

import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.IngredientResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.service.IngredientService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

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
        long start = System.currentTimeMillis();


        Map<String, Object> req = new LinkedHashMap<>();
        req.put("keyword", keyword);
        req.put("page", page);
        req.put("sort", sort);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Ingredient list request")
                .method("GET")
                .url("/api/ingredients")
                .requestBody(req)
                .lineCode("IngredientController#list")
        );

        var result = ingredientService.list(keyword, page, sort);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("page", result.getPage());
        res.put("size", result.getSize());
        res.put("total", result.getTotal());

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Ingredient list response")
                .method("GET")
                .url("/api/ingredients")
                .status(200)
                .responseBody(res)
                .duration((System.currentTimeMillis() - start))
                .lineCode("IngredientController#list")
        );

        return ApiResponse.<PageResponse<IngredientResponse>>builder()
                .code(1000)
                .message("Success")
                .result(result)
                .build();
    }
}
