package io.github.zlooo.fixyou.session;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ValidationConfig {

    public static final ValidationConfig DEFAULT =
            new ValidationConfig().setValidate(false)
                                  .setShouldCheckOrigVsSendingTime(false)
                                  .setShouldCheckSessionIDAfterLogon(false)
                                  .setShouldCheckBodyLength(false)
                                  .setShouldCheckSendingTime(false)
                                  .setShouldCheckMessageType(false)
                                  .setShouldCheckMessageChecksum(false);

    private boolean validate;
    private boolean shouldCheckOrigVsSendingTime;
    private boolean shouldCheckSessionIDAfterLogon;
    private boolean shouldCheckBodyLength;
    private boolean shouldCheckSendingTime;
    private boolean shouldCheckMessageType;
    private boolean shouldCheckMessageChecksum;
}
