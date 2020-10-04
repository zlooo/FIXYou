package io.github.zlooo.fixyou.fix.commons;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import lombok.Value;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DictionaryRepository {

    private final FIXYouConfiguration fixYouConfiguration;
    private Map<String, Dictionary> dictionaries = new HashMap<>();

    @Inject
    DictionaryRepository(FIXYouConfiguration fixYouConfiguration) {
        this.fixYouConfiguration = fixYouConfiguration;
    }

    public @Nullable
    Dictionary getDictionary(String dictionaryID) {
        return dictionaries.get(dictionaryID);
    }

    public void registerDictionary(String dictionaryID, FixSpec fixSpec, int fixMessageReadPoolSize, int fixMessageWritePoolSize) {
        if (dictionaries.putIfAbsent(dictionaryID, new Dictionary(fixSpec, createPool(fixSpec, fixMessageReadPoolSize), createPool(fixSpec, fixMessageWritePoolSize))) != null) {
            throw new FIXYouException("Dictionary with id " + dictionaryID + " already exist");
        }
    }

    private ObjectPool<FixMessage> createPool(FixSpec fixSpec, int poolSize) {
        return fixYouConfiguration.isSeparateIoFromAppThread() ? new ArrayBackedObjectPool<>(poolSize, () -> new FixMessage(fixSpec), FixMessage.class, poolSize) :
                new DefaultObjectPool<>(poolSize, () -> new FixMessage(fixSpec), FixMessage.class, poolSize);
    }

    @Value
    public static final class Dictionary {
        private final FixSpec fixSpec;
        private final ObjectPool<FixMessage> fixMessageReadPool;
        private final ObjectPool<FixMessage> fixMessageWritePool;
    }
}
