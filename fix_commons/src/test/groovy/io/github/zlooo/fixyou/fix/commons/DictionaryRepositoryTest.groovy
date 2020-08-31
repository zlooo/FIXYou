package io.github.zlooo.fixyou.fix.commons

import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.FIXYouException
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import org.assertj.core.api.Assertions
import spock.lang.Specification

class DictionaryRepositoryTest extends Specification {

    private DictionaryRepository dictionaryRepository = new DictionaryRepository(new FIXYouConfiguration.FIXYouConfigurationBuilder().build())
    private DictionaryRepository.Dictionary testDictionary1

    void setup() {
        testDictionary1 = new DictionaryRepository.Dictionary(TestSpec.INSTANCE, Mock(DefaultObjectPool), Mock(DefaultObjectPool))
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
        def dictionary2 = new DictionaryRepository.Dictionary(TestSpec.INSTANCE, Mock(DefaultObjectPool), Mock(DefaultObjectPool))

        when:
        dictionaryRepository.registerDictionary("testDictionary2", TestSpec.INSTANCE)

        then:
        Assertions.assertThat(dictionaryRepository.@dictionaries).contains(Assertions.entry("testDictionary1", testDictionary1)).hasSize(2).containsKey("testDictionary2")
        Assertions.assertThat(dictionaryRepository.@dictionaries["testDictionary2"]).isEqualToComparingOnlyGivenFields(dictionary2, "fixSpec")
        Assertions.assertThat(dictionaryRepository.@dictionaries["testDictionary2"]).matches({ it.fixMessageReadPool instanceof ArrayBackedObjectPool && it.fixMessageWritePool instanceof ArrayBackedObjectPool })
    }

    def "should register dictionary when io and app threads are not separated"() {
        setup:
        def dictionary2 = new DictionaryRepository.Dictionary(TestSpec.INSTANCE, Mock(DefaultObjectPool), Mock(DefaultObjectPool))
        dictionaryRepository = new DictionaryRepository(new FIXYouConfiguration.FIXYouConfigurationBuilder().separateIoFromAppThread(false).build())

        when:
        dictionaryRepository.registerDictionary("testDictionary2", TestSpec.INSTANCE)

        then:
        Assertions.assertThat(dictionaryRepository.@dictionaries).hasSize(1).containsKey("testDictionary2")
        Assertions.assertThat(dictionaryRepository.@dictionaries["testDictionary2"]).isEqualToComparingOnlyGivenFields(dictionary2, "fixSpec")
        Assertions.assertThat(dictionaryRepository.@dictionaries["testDictionary2"]).matches({ it.fixMessageReadPool instanceof DefaultObjectPool && it.fixMessageWritePool instanceof DefaultObjectPool })
    }

    def "should not register dictionary with same id twice"() {
        when:
        dictionaryRepository.registerDictionary("testDictionary1", TestSpec.INSTANCE)

        then:
        thrown(FIXYouException)
        Assertions.assertThat(dictionaryRepository.@dictionaries).containsOnly(Assertions.entry("testDictionary1", testDictionary1))
    }
}
