package io.github.zlooo.fixyou.parser.model;

import lombok.Getter;

@Getter
public class ValueNotSetException extends RuntimeException {

    private final int fieldNumber;

    public ValueNotSetException(int fieldNumber) {
        super("Value of field " + fieldNumber + " is not set");
        this.fieldNumber = fieldNumber;
    }
}
