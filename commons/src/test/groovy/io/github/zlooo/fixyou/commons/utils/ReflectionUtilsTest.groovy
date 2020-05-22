package io.github.zlooo.fixyou.commons.utils

import spock.lang.Specification

class ReflectionUtilsTest extends Specification {

    def "should set final field"(){
        setup:
        ClassWithFinalField classWithFinalField = new ClassWithFinalField()

        when:
        ReflectionUtils.setFinalField(classWithFinalField, "field", -1)

        then:
        classWithFinalField.@field==-1
    }

    def "should set final field which is declared in parent class"(){
        setup:
        ClassWithFinalFieldInSuper classWithFinalField = new ClassWithFinalFieldInSuper()

        when:
        ReflectionUtils.setFinalField(classWithFinalField, "field", -1)

        then:
        classWithFinalField.@field==-1
    }

    def "should throw exception when cannot find field to set"(){
        setup:
        ClassWithFinalField classWithFinalField = new ClassWithFinalField()

        when:
        ReflectionUtils.setFinalField(classWithFinalField, "wrongField", -1)

        then:
        thrown(NoSuchFieldException)
    }

    private static class ClassWithFinalField{
        protected final int field = 0
    }

    private static class ClassWithFinalFieldInSuper extends ClassWithFinalField{
        private int field2 = 666
    }
}
