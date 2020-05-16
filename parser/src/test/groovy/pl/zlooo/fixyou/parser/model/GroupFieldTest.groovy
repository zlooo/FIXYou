package pl.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.DefaultConfiguration
import pl.zlooo.fixyou.FIXYouException
import pl.zlooo.fixyou.model.FieldType
import pl.zlooo.fixyou.model.FixSpec
import pl.zlooo.fixyou.parser.FieldTestUtils
import pl.zlooo.fixyou.parser.TestSpec
import pl.zlooo.fixyou.parser.utils.FieldTypeUtils
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class GroupFieldTest extends Specification {

    private GroupField groupField = new GroupField(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

    def "should create a group field"() {
        setup:
        def longChildField = new FixSpec.FieldNumberTypePair(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER)
        def booleanChildField = new FixSpec.FieldNumberTypePair(FieldType.BOOLEAN, TestSpec.BOOLEAN_FIELD_NUMBER)
        def expectedLongfield = new LongField(TestSpec.LONG_FIELD_NUMBER)
        def expectedBooleanField = new BooleanField(TestSpec.BOOLEAN_FIELD_NUMBER)

        when:
        GroupField groupField = new GroupField(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.INSTANCE)

        then:
        groupField.numberOfFieldsInGroup() == 2
        Assertions.assertThat(groupField.@childFields).containsExactly(longChildField, booleanChildField)
        Assertions.assertThat(groupField.@repetitions).hasSize(DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP).doesNotContainNull()
        Assertions.assertThat(groupField.@repetitions[0].fieldsOrdered).usingElementComparator(FieldTestUtils.FIELD_COMPARATOR).containsOnly(expectedLongfield, expectedBooleanField)
        Assertions.assertThat(groupField.@repetitions[0].idToField).hasSize(2)
        Assertions.assertThat(groupField.@repetitions[0].idToField[TestSpec.LONG_FIELD_NUMBER]).usingComparator(FieldTestUtils.FIELD_COMPARATOR).isEqualTo(expectedLongfield)
        Assertions.assertThat(groupField.@repetitions[0].idToField[TestSpec.BOOLEAN_FIELD_NUMBER]).usingComparator(FieldTestUtils.FIELD_COMPARATOR).isEqualTo(expectedBooleanField)
        groupField.@repetitionSupplier != null
        groupField.@fieldDataWithoutRepetitionCount != null
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

    def "should get field by number"() {
        setup:
        groupField.write().getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        expect:
        FieldTestUtils.FIELD_COMPARATOR.compare(groupField.getField(0, expectedField.number), expectedField) == 0

        where:
        expectedField << [fieldWithValue(FieldType.LONG, TestSpec.LONG_FIELD_NUMBER, 666L), fieldWithValue(FieldType.BOOLEAN, TestSpec.BOOLEAN_FIELD_NUMBER, true)]
    }

    def "should update inner buffer once repetition is inputted"() {
        setup:
        groupField.write().getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        groupField.writeNext()

        then:
        groupField.@numberOfRepetitions == 1
        ByteBuf expectedBuffer = Unpooled.buffer(100)
        expectedBuffer.writeCharSequence("1\u0001${TestSpec.LONG_FIELD_NUMBER}=666\u0001${TestSpec.BOOLEAN_FIELD_NUMBER}=Y", StandardCharsets.US_ASCII)
        assert groupField.getFieldData().compareTo(expectedBuffer) == 0: "Expected buffer: ${expectedBuffer.toString(StandardCharsets.US_ASCII)}, but got ${groupField.getFieldData().toString(StandardCharsets.US_ASCII)}"
    }

    def "should update inner buffer once second repetition is inputted"() {
        setup:
        groupField.write().getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.BOOLEAN_FIELD_NUMBER).value = true
        groupField.writeNext()
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 667L
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        groupField.writeNext()

        then:
        groupField.@numberOfRepetitions == 2
        ByteBuf expectedBuffer = Unpooled.buffer(100)
        expectedBuffer.writeCharSequence("2\u0001${TestSpec.LONG_FIELD_NUMBER}=666\u0001${TestSpec.BOOLEAN_FIELD_NUMBER}=Y\u0001${TestSpec.LONG_FIELD_NUMBER}=667\u0001${TestSpec.BOOLEAN_FIELD_NUMBER}=Y", StandardCharsets.US_ASCII)
        assert groupField.getFieldData().compareTo(expectedBuffer) == 0: "Expected buffer: ${expectedBuffer.toString(StandardCharsets.US_ASCII)}, but got ${groupField.getFieldData().toString(StandardCharsets.US_ASCII)}"
    }

    def "should reset inner state"() {
        setup:
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).setValue(666L)
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.BOOLEAN_FIELD_NUMBER).setValue(true)
        groupField.writeNext()

        when:
        groupField.resetInnerState()

        then:
        groupField.@repetitions.collect { it.fieldsOrdered }.flatten().every { !it.isValueSet() }
        groupField.@numberOfRepetitions == 0
        groupField.@fieldDataWithoutRepetitionCount.readerIndex() == 0
        groupField.@fieldDataWithoutRepetitionCount.writerIndex() == 0
    }

    def "should parse repetition count"() {
        setup:
        groupField.getFieldData().writeCharSequence("666", StandardCharsets.US_ASCII)

        when:
        groupField.parseRepetitionsNumber()

        then:
        groupField.@numberOfRepetitionsRead == 666
        Assertions.assertThat(groupField.@repetitions).hasSize(666).doesNotContainNull()
    }

    def "should close child fields when closed"() {
        when:
        groupField.close()

        then:
        groupField.@fieldDataWithoutRepetitionCount.refCnt() == 0
        groupField.@repetitions.collect { it.fieldsOrdered }.flatten().collect { it.fieldData }.every { it.refCnt() == 0 }
    }

    def "should not increase number of repetitions on consecutive get field calls when field value is not set"() {
        when:
        def result = groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.@numberOfRepetitions == 0
        !result.isValueSet()
    }

    def "should increase number of repetitions on consecutive get field calls when field value is set"() {
        setup:
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.@numberOfRepetitionsRead = 2

        when:
        def result = groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER)

        then:
        groupField.@numberOfRepetitions == 1
        !result.isValueSet()
    }

    def "should throw exception if when too many repetitions are requested to be read"() {
        setup:
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER).value = 666L
        groupField.@numberOfRepetitionsRead = 1

        when:
        groupField.getFieldAndIncRepetitionIfValueIsSet(TestSpec.LONG_FIELD_NUMBER)

        then:
        thrown(FIXYouException)
    }

    private static <T extends AbstractField> T fieldWithValue(FieldType fieldType, int number, Object value) {
        def field = FieldTypeUtils.createField(fieldType, number, TestSpec.INSTANCE)
        field.setValue(value)
        return field
    }
}