package io.github.zlooo.fixyou.commons;

import lombok.Getter;

@Getter
public class ByteBufComposerIndexOutOfBoundsException extends java.lang.IndexOutOfBoundsException {

    private final transient ByteBufComposer source;

    public ByteBufComposerIndexOutOfBoundsException(String message, ByteBufComposer source) {
        super(message + ", composer: " + source);
        this.source = source;
    }
}
