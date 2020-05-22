package io.github.zlooo.fixyou.fix.commons;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RejectReasons {

    public static final int INVALID_TAG_NUMBER = 0;
    public static final int REQUIRED_TAG_MISSING = 1;
    public static final int TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE = 2;
    public static final int UNDEFINED_TAG = 3;
    public static final int TAG_SPECIFIED_WITHOUT_A_VALUE = 4;
    public static final int VALUE_IS_INCORRECT_FOR_THIS_TAG = 5;
    public static final int INCORRECT_DATA_FORMAT_FOR_VALUE = 6;
    public static final int DECRYPTION_PROBLEM = 7;
    public static final int SIGNATURE_PROBLEM = 8;
    public static final int COMP_ID_PROBLEM = 9;
    public static final int SENDING_TIME_ACCURACY_PROBLEM = 10;
    public static final int INVALID_MESSAGE_TYPE = 11;
    public static final int XML_VALIDATION_ERROR = 12;
    public static final int TAG_APPEARS_MORE_THAN_ONCE = 13;
    public static final int TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER = 14;
    public static final int REPEATING_GROUP_FIELDS_OUT_OF_ORDER = 15;
    public static final int INCORRECT_NUM_IN_GROUP_COUNT_FOR_REPEATING_GROUP = 16;
    public static final int NON_DATA_VALUE_INCLUDES_FIELD_DELIMITER = 17;
    public static final int OTHER = 99;

    public static final char[] TOO_LOW_NEW_SEQUENCE_NUMBER = new char[]{'S', 'e', 'q', 'u', 'e', 'n', 'c', 'e', ' ', 'n', 'u', 'm', 'b', 'e', 'r', ' ', 'p', 'r', 'o', 'v', 'i', 'd', 'e', 'd', ' ',
            'i', 'n', ' ', 'N', 'e', 'w', 'S', 'e', 'q', 'N', 'o', '(', '3', '6', ')', ' ', 'f', 'i', 'e', 'l', 'd', ' ', 'i', 's', ' ', 'l', 'o', 'w', 'e', 'r', ' ', 't', 'h',
            'a', 'n', ' ', 'e', 'x', 'p', 'e', 'c', 't', 'e', 'd', ' ', 'v', 'a', 'l', 'u', 'e'};
    public static final char[] INVALID_LOGON_MESSAGE = new char[]{'I', 'n', 'v', 'a', 'l', 'i', 'd', ' ', 'l', 'o', 'g', 'o', 'n', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e'};
}
