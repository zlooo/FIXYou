package io.github.zlooo.fixyou.netty.handler;

public enum Handlers {

    MESSAGE_DECODER("messageDecoder"), MESSAGE_ENCODER("messageEncoder"), SESSION("sessionHandler"), ADMIN_MESSAGES("adminMessagesHandler"), GENERIC("genericHandler"),
    GENERIC_DECODER("genericDecoder"), LISTENER_INVOKER("fixMessageListenerInvoker"), BEFORE_SESSION_MESSAGE_VALIDATOR("beforeSessionEstablishedMessageValidator"),
    AFTER_SESSION_MESSAGE_VALIDATOR("afterSessionEstablishedMessageValidator"), IDLE_STATE_HANDLER("idleStateHandler"), MESSAGE_STORE_HANDLER("messageStore"), FLUSH_CONSOLIDATION_HANDLER("flushConsolidationHandler");
    private final String name;

    Handlers(String name) {
        this.name = name;
    }

    public java.lang.String getName() {
        return name;
    }
}
