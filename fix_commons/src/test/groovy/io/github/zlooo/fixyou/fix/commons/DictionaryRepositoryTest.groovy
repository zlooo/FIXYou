package io.github.zlooo.fixyou.fix.commons

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import org.assertj.core.api.Assertions
import spock.lang.Specification

class DictionaryRepositoryTest extends Specification {

    private DictionaryRepository dictionaryRepository = new DictionaryRepository()
    private DictionaryRepository.Dictionary testDictionary1

    void setup() {
        testDictionary1 = new DictionaryRepository.Dictionary(TestSpec.INSTANCE, Mock(DefaultObjectPool))
        dictionaryRepository.@dictionaries.put("testDictionary1", testDictionary1)
    }

    def "should get dictionary"() {
        expect:
        dictionaryRepository.getDictionary("testDictionary1") == testDictionary1
    }

    def "should not get not existing dictionary"() {
        expect:
        dictionaryRepository.getDictionary("fakeDictionary") == null
    }

    def "should register dictionary"() {
        setup:
        def dictionary2 = new DictionaryRepository.Dictionary(TestSpec.INSTANCE, Mock(DefaultObjectPool))

        when:
        dictionaryRepository.registerDictionary("testDictionary2", TestSpec.INSTANCE)

        then:
        Assertions.assertThat(dictionaryRepository.@dictionaries).contains(Assertions.entry("testDictionary1", testDictionary1)).hasSize(2).containsKey("testDictionary2")
        Assertions.assertThat(dictionaryRepository.@dictionaries["testDictionary2"]).isEqualToComparingOnlyGivenFields(dictionary2, "fixSpec")
    }

    def "should not register dictionary with same id twice"() {
        when:
        dictionaryRepository.registerDictionary("testDictionary1", TestSpec.INSTANCE)

        then:
        thrown(io.github.zlooo.fixyou.FIXYouException)
        Assertions.assertThat(dictionaryRepository.@dictionaries).containsOnly(Assertions.entry("testDictionary1", testDictionary1))
    }
}
