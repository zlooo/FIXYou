package io.github.zlooo.fixyou.commons;

import lombok.Getter;

@Getter
public class IndexOutOfBoundsException extends java.lang.IndexOutOfBoundsException {

    private final ByteBufComposer source;

    public IndexOutOfBoundsException(String message, ByteBufComposer source) {
        super(message + ", composer: " + source);
        this.source = source;
    }
}
