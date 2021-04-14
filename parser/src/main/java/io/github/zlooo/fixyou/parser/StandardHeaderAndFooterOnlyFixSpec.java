package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.model.ApplicationVersionID;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;

import javax.annotation.Nonnull;

public class StandardHeaderAndFooterOnlyFixSpec implements FixSpec {

    public static final StandardHeaderAndFooterOnlyFixSpec INSTANCE = new StandardHeaderAndFooterOnlyFixSpec();
    private static final int[] HEADER_FIELDS = new int[]{8, 9, 35, 49, 56, 34, 52};
    private static final int[] BODY_FIELDS = new int[]{10};
    private static final FieldType[] HEADER_TYPES =
            new FieldType[]{FixFieldsTypes.BEGIN_STRING, FixFieldsTypes.BODY_LENGTH, FixFieldsTypes.MESSAGE_TYPE, FixFieldsTypes.SENDER_COMP_ID, FixFieldsTypes.TARGET_COMP_ID, FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER,
                    FixFieldsTypes.SENDING_TIME};
    private static final FieldType[] BODY_TYPES = new FieldType[]{FixFieldsTypes.CHECK_SUM};

    @Nonnull
    @Override
    public int[] getHeaderFieldsOrder() {
        return HEADER_FIELDS;
    }

    @Nonnull
    @Override
    public FieldType[] getHeaderFieldTypes() {
        return HEADER_TYPES;
    }

    @Nonnull
    @Override
    public int[] getBodyFieldsOrder() {
        return BODY_FIELDS;
    }

    @Nonnull
    @Override
    public FieldType[] getBodyFieldTypes() {
        return BODY_TYPES;
    }

    @Nonnull
    @Override
    public char[][] getMessageTypes() {
        return ArrayUtils.EMPTY_TWO_DIM_CHAR;
    }

    @Override
    public ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2;
    }

    @Override
    public FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return ArrayUtils.EMPTY_FIELD_NUMBER_TYPE;
    }
}
