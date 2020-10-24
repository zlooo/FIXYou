package io.github.zlooo.fixyou.parser.model;

import lombok.Getter;

public class FieldValueNotSetException extends RuntimeException {

    @Getter
    private final Field field;

    public FieldValueNotSetException(Field field) {
        super("Trying to serialize a value from a field that actually do not have value set. Field details: " + field.toString());
        this.field = field;
    }
}
