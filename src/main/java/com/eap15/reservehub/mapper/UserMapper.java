package com.eap15.reservehub.mapper;

import com.eap15.reservehub.dto.UserDTO;
import com.eap15.reservehub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Ignora la contraseña al convertir a DTO (nunca exponer el hash)
    @Mapping(target = "password", ignore = true)
    UserDTO toDTO(User user);

    // Al convertir de DTO a Entity, el id lo genera la BD
    @Mapping(target = "id", ignore = true)
    User toEntity(UserDTO userDTO);
}