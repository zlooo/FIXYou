package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.DefaultConfiguration
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class GroupFieldTest extends Specification {

    private Field groupField = new Field(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, new FieldCodec())

    def "should create a group field"() {
        setup:
        def expectedLongField = new Field(TestSpec.LONG_FIELD_NUMBER, new FieldCodec())
        def expectedBooleanField = new Field(TestSpec.BOOLEAN_FIELD_NUMBER, new FieldCodec())

        when:
        Field groupField = new Field(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, new FieldCodec())

        then:
        groupField.@fieldValue.repetitionCounter == 0
        Assertions.assertThat(groupField.@fieldValue.repetitions).hasSize(DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP).doesNotContainNull()
        if (DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP > 0) {
            Assertions.assertThat(groupField.@fieldValue.repetitions[0].idToField).containsOnly(expectedLongField, expectedBooleanField)
            Assertions.assertThat(groupField.@fieldValue.repetitions[0].idToField).hasSize(2)
            Assertions.assertThat(groupField.@fieldValue.repetitions[0].idToField[TestSpec.LONG_FIELD_NUMBER]).isEqualTo(expectedLongField)
            Assertions.assertThat(groupField.@fieldValue.repetitions[0].idToField[TestSpec.BOOLEAN_FIELD_NUMBER]).isEqualTo(expectedBooleanField)
        }
    }

    def "should set message byte source on all child fields"() {
        setup:
        ByteBufComposer messageByteSource = Mock()

        when:
        groupField.fieldData = messageByteSource

        then:
        groupField.fieldData == messageByteSource
        Assertions.assertThat(groupField.@fieldValue.repetitions).allMatch({ it.fieldsOrdered.every { it.fieldData == messageByteSource } })
    }

    def "should reset field state"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).setLongValue(666L)
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).setBooleanValue(true)
        groupField.endCurrentRepetition()

        when:
        groupField.reset()

        then:
        groupField.@fieldValue.repetitions.collect { it.idToField.values() }.flatten().every { !it.isValueSet() }
        groupField.@fieldValue.repetitionCounter == 0
        groupField.@fieldValue.longValue == FieldValue.LONG_DEFAULT_VALUE
    }

    def "should get field from current repetition"() {
        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.@fieldValue.repetitionCounter == 0
        result.number == TestSpec.LONG_FIELD_NUMBER
        !result.valueSet
    }

    def "should get field from next repetition"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666L
        groupField.endCurrentRepetition()

        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.@fieldValue.repetitionCounter == 1
        result.number == TestSpec.LONG_FIELD_NUMBER
        !result.valueSet
    }

    def "should expand repetitions array when needed"() {
        setup:
        def repetitionsArrayLength = groupField.@fieldValue.repetitions.length
        (0..repetitionsArrayLength).forEach({
            groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666L
            groupField.endCurrentRepetition()
        })

        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.@fieldValue.repetitionCounter == repetitionsArrayLength + 1
        result.number == TestSpec.LONG_FIELD_NUMBER
        !result.valueSet
        groupField.@fieldValue.repetitions.length > repetitionsArrayLength
    }

    def "should get zero field value"() {
        expect:
        groupField.longValue == FieldValue.LONG_DEFAULT_VALUE
    }

    def "should increase counters when endCurrentRepetition is called"() {
        when:
        groupField.endCurrentRepetition()

        then:
        groupField.longValue == FieldValue.LONG_DEFAULT_VALUE
        groupField.@fieldValue.repetitionCounter == 1
    }

    def "should not claim value is set"() {
        expect:
        !groupField.valueSet
    }

    def "should not claim value is set when just accessing field"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        expect:
        !groupField.valueSet
    }

    def "should claim value is set"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
        groupField.endCurrentRepetition()

        expect:
        groupField.valueSet
    }

    def "should claim value is set with multiple repetitions"() {
        setup:
        (1..numberOfRepetitions).forEach({
            groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
            groupField.endCurrentRepetition()
        })

        expect:
        groupField.valueSet

        where:
        numberOfRepetitions | _
        1                   | _
        2                   | _
        3                   | _
        10                  | _
        66                  | _
        100                 | _
    }

    def "should append child field values to buffer"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).booleanValue = true
        groupField.endCurrentRepetition()
        def buffer = Unpooled.buffer(100, 100)

        when:
        def result = groupField.appendByteBufWithValue(buffer, TestSpec.INSTANCE)

        then:
        buffer.toString(StandardCharsets.US_ASCII) == "1\u00011=666\u00012=Y"
        result == TestUtils.sumBytes("1\u00011=666\u00012=Y".getBytes(StandardCharsets.US_ASCII))
    }

    def "should append child field values to buffer when multiple repetitions are stored"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).booleanValue = true
        groupField.endCurrentRepetition()
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
        groupField.endCurrentRepetition()
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).longValue = 666
        groupField.endCurrentRepetition()
        def buffer = Unpooled.buffer(300, 300)

        when:
        def result = groupField.appendByteBufWithValue(buffer, TestSpec.INSTANCE)

        then:
        buffer.toString(StandardCharsets.US_ASCII) == "3\u00011=666\u00012=Y\u00011=666\u00011=666"
        result == TestUtils.sumBytes("3\u00011=666\u00012=Y\u00011=666\u00011=666".getBytes(StandardCharsets.US_ASCII))
    }

    def "should close child fields when group field is closed"() {
        setup:
        def field = Mock(Field)
        groupField.@fieldValue.repetitionCounter = 1
        groupField.@fieldValue.ensureRepetitionsArrayCapacity()
        def repetitions = groupField.@fieldValue.repetitions
        def fieldCounter = 0
        (0..repetitions.length - 1).forEach { rep ->
            (0..9).forEach {
                repetitions[rep].getExistingOrNewGroupField(it, new FieldCodec())
                repetitions[rep].@idToField.put(it, field)
                fieldCounter++
            }
        }

        when:
        groupField.close()

        then:
        fieldCounter * field.close()
        0 * _
    }

    private static <T extends Field> T fieldWithValue(FieldType fieldType, int number, Object value = null) {
        def field = new Field(number, new FieldCodec())
        if (value != null) {
            switch (fieldType) {
                case FieldType.BOOLEAN: field.booleanValue = value; break
                case FieldType.CHAR: field.charValue = value; break
                case FieldType.CHAR_ARRAY: field.charSequenceValue = value; break
                case FieldType.DOUBLE: field.setDoubleValue(value, 0 as short); break
                case FieldType.LONG: field.longValue = value; break
                case FieldType.TIMESTAMP: field.timestampValue = value; break
                default: throw new IllegalArgumentException("Unsupported field type $fieldType")
            }
        }
        return field
    }

}
