package com.vule.authen.service;

import com.vule.authen.dto.response.IngredientResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.entity.Ingredient;
import com.vule.authen.repository.IngredientRepository;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngredientService {

    IngredientRepository ingredientRepository;

    public PageResponse<IngredientResponse> list(String keyword, Integer page, String sort) {
        long start = System.currentTimeMillis();

        int pageDefault = 1;
        int sizeDefault = 20;

        int p = (page == null) ? pageDefault : page;
        if (p < 1) p = 1;

        String sortNormalized;
        Sort s;
        if (sort == null || sort.isBlank() || sort.equalsIgnoreCase("price")) {
            sortNormalized = "price,asc";
            s = Sort.by(Sort.Direction.ASC, "price");
        } else if (sort.equalsIgnoreCase("price,desc")) {
            sortNormalized = "price,desc";
            s = Sort.by(Sort.Direction.DESC, "price");
        } else {
            sortNormalized = "price,asc";
            s = Sort.by(Sort.Direction.ASC, "price");
        }

        Pageable pageable = PageRequest.of(p - 1, sizeDefault, s);

        Map<String, Object> ctxReq = new LinkedHashMap<>();
        ctxReq.put("keyword", keyword);
        ctxReq.put("pageInput", page);
        ctxReq.put("pageNormalized", p);
        ctxReq.put("size", sizeDefault);
        ctxReq.put("sortInput", sort);
        ctxReq.put("sortNormalized", sortNormalized);

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("IngredientService.list called")
                .context(ctxReq)
                .lineCode("IngredientService#list")
        );

        Page<Ingredient> result;
        try {
            result = ingredientRepository.search(keyword, pageable);
        } catch (Exception ex) {
            Map<String, Object> ctxErr = new LinkedHashMap<>();
            ctxErr.put("keyword", keyword);
            ctxErr.put("page", p);
            ctxErr.put("size", sizeDefault);
            ctxErr.put("sort", sortNormalized);

            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("IngredientRepository: search ==> Failed")
                            .context(ctxErr)
                            .duration(System.currentTimeMillis() - start)
                            .lineCode("IngredientService#list")
                    , ex);

            throw ex;
        }

        var response = new PageResponse<>(
                p,
                sizeDefault,
                result.getTotalElements(),
                result.getContent().stream()
                        .map(i -> new IngredientResponse(
                                i.getId(),
                                i.getCode(),
                                i.getName(),
                                i.getPrice(),
                                i.getNumber()
                        ))
                        .toList()
        );

        Map<String, Object> ctxRes = new LinkedHashMap<>();
        ctxRes.put("totalElements", result.getTotalElements());
        ctxRes.put("returnedSize", result.getContent() == null ? 0 : result.getContent().size());
        ctxRes.put("page", p);
        ctxRes.put("size", sizeDefault);
        ctxRes.put("sort", sortNormalized);

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("IngredientService: list ==> success")
                .context(ctxRes)
                .duration(System.currentTimeMillis() - start)
                .lineCode("IngredientService#list")
        );

        return response;
    }
}
