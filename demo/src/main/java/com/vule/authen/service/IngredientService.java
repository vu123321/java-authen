package com.vule.authen.service;

import com.vule.authen.dto.response.IngredientResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.entity.Ingredient;
import com.vule.authen.repository.IngredientRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngredientService {
    IngredientRepository ingredientRepository;

    public PageResponse<IngredientResponse> list(String keyword, Integer page, String sort) {

        int pageDefault = 1;
        int sizeDefault = 20;

        int p = (page == null) ? pageDefault : page;
        if (p < 1) p = 1;

        Sort s;
        if (sort == null || sort.isBlank() || sort.equalsIgnoreCase("price")) {
            s = Sort.by(Sort.Direction.ASC, "price");
        } else if (sort.equalsIgnoreCase("price,desc")) {
            s = Sort.by(Sort.Direction.DESC, "price");
        } else {
            s = Sort.by(Sort.Direction.ASC, "price");
        }

        Pageable pageable = PageRequest.of(p - 1, sizeDefault, s);

        Page<Ingredient> result = ingredientRepository.search(keyword, pageable);

        return new PageResponse<>(
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
    }
}
