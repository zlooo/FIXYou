package pl.zlooo.fixyou.fix.commons;

import lombok.Getter;
import lombok.ToString;
import pl.zlooo.fixyou.parser.model.FixMessage;

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
