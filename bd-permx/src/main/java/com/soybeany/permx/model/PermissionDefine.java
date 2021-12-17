package com.soybeany.permx.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Soybeany
 * @date 2021/5/26
 */
@SuppressWarnings("unused")
@ToString
@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class PermissionDefine {

    private final String name;
    private final String value;
    private final String description;

    public PermissionParts getParts() {
        return PermissionParts.parse(value);
    }

}
