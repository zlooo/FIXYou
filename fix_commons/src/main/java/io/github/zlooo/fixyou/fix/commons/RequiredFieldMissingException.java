package io.github.zlooo.fixyou.fix.commons;

import io.github.zlooo.fixyou.model.FixMessage;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class RequiredFieldMissingException extends RuntimeException {

    private final int fieldNumber;
    private final transient FixMessage fixMessage;

    public RequiredFieldMissingException(int fieldNumber, FixMessage fixMessage) {
        super("Required field(" + fieldNumber + ") is missing");
        this.fieldNumber = fieldNumber;
        this.fixMessage = fixMessage;
    }
}
