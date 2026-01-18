package com.vule.authen.mapper;

import com.vule.authen.dto.UserResponse;
import com.vule.authen.dto.request.UserCreationRequest;
import com.vule.authen.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);
}
