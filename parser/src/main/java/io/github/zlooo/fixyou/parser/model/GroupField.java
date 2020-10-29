package io.github.zlooo.fixyou.parser.model;

public interface GroupField {
    Field getFieldForCurrentRepetition(int fieldNum);

    Field getFieldForGivenRepetition(int repetitionIndex, int fieldNum);

    Field endCurrentRepetition();
}
