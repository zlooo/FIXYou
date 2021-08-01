package io.github.zlooo.fixyou.session;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ValidationConfig {

    public static final ValidationConfig DEFAULT =
            ValidationConfig.builder().validate(false)
                            .shouldCheckOrigVsSendingTime(false)
                            .shouldCheckSessionIDAfterLogon(false)
                            .shouldCheckBodyLength(false)
                            .shouldCheckSendingTime(false)
                            .shouldCheckMessageType(false)
                            .shouldCheckMessageChecksum(false).build();

    private boolean validate;
    private boolean shouldCheckOrigVsSendingTime;
    private boolean shouldCheckSessionIDAfterLogon;
    private boolean shouldCheckBodyLength;
    private boolean shouldCheckSendingTime;
    private boolean shouldCheckMessageType;
    private boolean shouldCheckMessageChecksum;
}
