package io.github.zlooo.fixyou;

import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class FixConstants {

    public static final int BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER = 7;
    public static final int BEGIN_STRING_FIELD_NUMBER = 8;
    public static final int BODY_LENGTH_FIELD_NUMBER = 9;
    public static final int CHECK_SUM_FIELD_NUMBER = 10;
    public static final int END_SEQUENCE_NUMBER_FIELD_NUMBER = 16;
    public static final int MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER = 34;
    public static final int MESSAGE_TYPE_FIELD_NUMBER = 35;
    public static final int NEW_SEQUENCE_NUMBER_FIELD_NUMBER = 36;
    public static final int POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER = 43;
    public static final int REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER = 45;
    public static final int SENDER_COMP_ID_FIELD_NUMBER = 49;
    public static final int SENDING_TIME_FIELD_NUMBER = 52;
    public static final int TARGET_COMP_ID_FIELD_NUMBER = 56;
    public static final int TEXT_FIELD_NUMBER = 58;
    public static final int ENCRYPT_METHOD_FIELD_NUMBER = 98;
    public static final int HEARTBEAT_INTERVAL_FIELD_NUMBER = 108;
    public static final int TEST_REQ_ID_FIELD_NUMBER = 112;
    public static final int ORIG_SENDING_TIME_FIELD_NUMBER = 122;
    public static final int GAP_FILL_FLAG_FIELD_NUMBER = 123;
    public static final int RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER = 141;
    public static final int REFERENCED_TAG_ID_FIELD_NUMBER = 371;
    public static final int SESSION_REJECT_REASON_FIELD_NUMBER = 373;
    public static final int USERNAME_FIELD_NUMBER = 553;
    public static final int PASSWORD_FIELD_NUMBER = 554;
    public static final int APPL_VERSION_ID_FIELD_NUMBER = 1128;
    public static final int DEFAULT_APP_VERSION_ID_FIELD_NUMBER = 1137;
    public static final char[] HEARTBEAT = {'0'};
    public static final char[] TEST_REQUEST = {'1'};
    public static final char[] RESEND_REQUEST = {'2'};
    public static final char[] REJECT = {'3'};
    public static final char[] LOGOUT = {'5'};
    public static final char[] LOGON = {'A'};
    public static final char[] SEQUENCE_RESET = {'4'};
    public static final long ENCRYPTION_METHOD_NONE = 0;
    public static final long ENCRYPTION_METHOD_PKCS = 1;
    public static final long ENCRYPTION_METHOD_DES = 2;
    public static final long ENCRYPTION_METHOD_PKCS_DES = 3;
    public static final long ENCRYPTION_METHOD_PGP_DES = 4;
    public static final long ENCRYPTION_METHOD_PGP_DES_MD5 = 5;
    public static final long ENCRYPTION_METHOD_PEM_DES_MD5 = 6;
    public static final DateTimeFormatter UTC_TIMESTAMP_NO_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");
    public static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd-HH:mm:ss.SSS";
    public static final DateTimeFormatter UTC_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(UTC_TIMESTAMP_PATTERN);
    public static final long SENDING_TIME_ACCURACY_MILLIS = TimeUnit.SECONDS.toMillis(30);
    public static final int CHECK_SUM_MODULO_MASK = 255;
    public static final char[] ADMIN_MESSAGE_TYPES = {'0', 'A', '5', '3', '2', '4', '1'};
}
