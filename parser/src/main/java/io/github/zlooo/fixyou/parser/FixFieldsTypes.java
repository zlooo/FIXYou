package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.model.FieldType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FixFieldsTypes {
    public static final FieldType MESSAGE_TYPE = FieldType.CHAR_ARRAY;
    public static final FieldType MESSAGE_SEQUENCE_NUMBER = FieldType.LONG;
    public static final FieldType SENDER_COMP_ID = FieldType.CHAR_ARRAY;
    public static final FieldType TARGET_COMP_ID = FieldType.CHAR_ARRAY;
    public static final FieldType NEW_SEQUENCE_NUMBER = FieldType.LONG;
    public static final FieldType GAP_FILL_FLAG = FieldType.BOOLEAN;
    public static final FieldType REFERENCED_TAG_ID = FieldType.LONG;
    public static final FieldType TEXT = FieldType.CHAR_ARRAY;
    public static final FieldType POSS_DUP_FLAG = FieldType.BOOLEAN;
    public static final FieldType CHECK_SUM = FieldType.LONG;
    public static final FieldType BEGIN_SEQUENCE_NUMBER = FieldType.LONG;
    public static final FieldType END_SEQUENCE_NUMBER = FieldType.LONG;
    public static final FieldType REFERENCED_SEQUENCE_NUMBER = FieldType.LONG;
    public static final FieldType SESSION_REJECT_REASON = FieldType.LONG;
    public static final FieldType ENCRYPT_METHOD = FieldType.LONG;
    public static final FieldType HEARTBEAT_INTERVAL = FieldType.LONG;
    public static final FieldType DEFAULT_APP_VERSION = FieldType.CHAR_ARRAY;
    public static final FieldType BEGIN_STRING = FieldType.CHAR_ARRAY;
    public static final FieldType BODY_LENGTH = FieldType.LONG;
    public static final FieldType SENDING_TIME = FieldType.CHAR_ARRAY;
    public static final FieldType USERNAME = FieldType.CHAR_ARRAY;
    public static final FieldType PASSWORD = FieldType.CHAR_ARRAY;
    public static final FieldType DEFAULT_APP_VERSION_ID = FieldType.CHAR_ARRAY;
    public static final FieldType APPL_VERSION_ID = FieldType.CHAR_ARRAY;
    public static final FieldType RESET_SEQ_NUMBER_FLAG = FieldType.BOOLEAN;
    public static final FieldType TEST_REQ_ID = FieldType.CHAR_ARRAY;
}
