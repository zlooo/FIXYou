package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.DefaultConfiguration
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class GroupFieldTest extends Specification {

    private GroupField groupField = new GroupField(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

    def "should create a group field"() {
        setup:
        def expectedLongfield = new LongField(TestSpec.LONG_FIELD_NUMBER)
        def expectedBooleanField = new BooleanField(TestSpec.BOOLEAN_FIELD_NUMBER)

        when:
        GroupField groupField = new GroupField(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

        then:
        groupField.repetitionCounter == 0
        Assertions.assertThat(groupField.@memberNumbers).containsExactly(TestSpec.LONG_FIELD_NUMBER, TestSpec.BOOLEAN_FIELD_NUMBER)
        Assertions.assertThat(groupField.@repetitions).hasSize(DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP).doesNotContainNull()
        if (DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP > 0) {
            Assertions.assertThat(groupField.@repetitions[0].fieldsOrdered).containsOnly(expectedLongfield, expectedBooleanField)
            Assertions.assertThat(groupField.@repetitions[0].idToField).hasSize(2)
            Assertions.assertThat(groupField.@repetitions[0].idToField[TestSpec.LONG_FIELD_NUMBER]).isEqualTo(expectedLongfield)
            Assertions.assertThat(groupField.@repetitions[0].idToField[TestSpec.BOOLEAN_FIELD_NUMBER]).isEqualTo(expectedBooleanField)
        }
        groupField.@repetitionSupplier != null
    }

    def "should not create group field when group is empty"() {
        when:
        new GroupField(TestSpec.EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

        then:
        thrown(IllegalArgumentException)
    }

    def "should not create group field when group is null"() {
        when:
        new GroupField(TestSpec.NULL_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

        then:
        thrown(IllegalArgumentException)
    }

    def "should set message byte source on all child fields"() {
        setup:
        ByteBufComposer messageByteSource = Mock()

        when:
        groupField.fieldData = messageByteSource

        then:
        groupField.fieldData == messageByteSource
        Assertions.assertThat(groupField.@repetitions).allMatch({ it.fieldsOrdered.every { it.fieldData == messageByteSource } })
    }

    def "should reset field state"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).setValue(666L)
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).setValue(true)

        when:
        groupField.reset()

        then:
        groupField.@repetitions.collect { it.fieldsOrdered }.flatten().every { !it.isValueSet() }
        groupField.repetitionCounter == 0
        groupField.value == GroupField.DEFAULT_VALUE
    }

    def "should get field by number"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        expect:
        groupField.getField(0, expectedField.number) == expectedField

        where:
        expectedField << [fieldWithValue(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER, 666L), fieldWithValue(FieldType.BOOLEAN, TestSpec.BOOLEAN_FIELD_NUMBER, true)]
    }

    def "should get field from current repetition"() {
        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.repetitionCounter == 0
        result == fieldWithValue(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER)
        !result.valueSet
    }

    def "should get field from next repetition"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.next()

        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.repetitionCounter == 1
        result == fieldWithValue(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER)
        !result.valueSet
    }

    def "should expand repetitions array when needed"() {
        setup:
        def repetitionsArrayLength = groupField.@repetitions.length
        (0..repetitionsArrayLength).forEach({
            groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666L
            groupField.next()
        })

        when:
        def result = groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.repetitionCounter == repetitionsArrayLength + 1
        result == fieldWithValue(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER)
        !result.valueSet
        groupField.@repetitions.length > repetitionsArrayLength
    }

    def "should get zero field value"() {
        expect:
        groupField.value == 0
    }

    def "should increase counters when next is called"() {
        when:
        groupField.next()

        then:
        groupField.value == 0
        groupField.repetitionCounter == 1
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
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666

        expect:
        groupField.valueSet
    }

    def "should claim value is set with multiple repetitions"() {
        setup:
        (1..numberOfRepetitions).forEach({
            groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666
            groupField.next()
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
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).value = true
        groupField.next()
        def buffer = Unpooled.buffer(100, 100)

        when:
        def result = groupField.appendByteBufWithValue(buffer)

        then:
        buffer.toString(StandardCharsets.US_ASCII) == "1\u00011=666\u00012=Y"
        result == TestUtils.sumBytes("1\u00011=666\u00012=Y".getBytes(StandardCharsets.US_ASCII))
    }

    def "should append child field values to buffer when multiple repetitions are stored"() {
        setup:
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666
        groupField.getFieldForCurrentRepetition(TestSpec.BOOLEAN_FIELD_NUMBER).value = true
        groupField.next()
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666
        groupField.next()
        groupField.getFieldForCurrentRepetition(TestSpec.LONG_FIELD_NUMBER).value = 666
        groupField.next()
        def buffer = Unpooled.buffer(300, 300)

        when:
        def result = groupField.appendByteBufWithValue(buffer)

        then:
        buffer.toString(StandardCharsets.US_ASCII) == "3\u00011=666\u00012=Y\u00011=666\u00011=666"
        result == TestUtils.sumBytes("3\u00011=666\u00012=Y\u00011=666\u00011=666".getBytes(StandardCharsets.US_ASCII))
    }

    def "should close child fields when group field is closed"() {
        setup:
        def field = Mock(AbstractField)
        groupField.repetitionCounter = 1
        groupField.ensureRepetitionsArrayCapacity()
        def repetitions = groupField.@repetitions
        def fieldCounter = 0
        (0..repetitions.length - 1).forEach {
            def fieldsOrdered = repetitions[it].fieldsOrdered
            (0..fieldsOrdered.length - 1).forEach {
                fieldsOrdered[it] = field
                fieldCounter++
            }
        }

        when:
        groupField.close()

        then:
        fieldCounter * field.close()
        0 * _
    }

    def "should check if group contains field with given number"() {
        expect:
        groupField.containsField(fieldNumber) == expectedResult

        where:
        fieldNumber                   | expectedResult
        TestSpec.LONG_FIELD_NUMBER    | true
        TestSpec.BOOLEAN_FIELD_NUMBER | true
        666                           | false
    }

    private static <T extends AbstractField> T fieldWithValue(FieldType fieldType, int number, Object value = null) {
        def field = FieldTypeUtils.createField(fieldType, number, TestSpec.INSTANCE)
        if (value != null) {
            field.setValue(value)
        }
        return field
    }

}
