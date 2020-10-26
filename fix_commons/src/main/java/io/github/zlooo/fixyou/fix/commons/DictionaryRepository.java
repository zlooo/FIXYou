package io.github.zlooo.fixyou.fix.commons;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.FieldCodec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import lombok.Value;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DictionaryRepository { //TODO this class will not be needed any more one I make fix message spec agnostic, there will be just 1 pool

    private final FIXYouConfiguration fixYouConfiguration;
    private final FieldCodec fieldCodec;
    private Map<String, Dictionary> dictionaries = new HashMap<>();

    @Inject
    DictionaryRepository(FIXYouConfiguration fixYouConfiguration, FieldCodec fieldCodec) {
        this.fixYouConfiguration = fixYouConfiguration;
        this.fieldCodec = fieldCodec;
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
        return fixYouConfiguration.isSeparateIoFromAppThread() ? new ArrayBackedObjectPool<>(poolSize, () -> new FixMessage(fieldCodec), FixMessage.class, poolSize) :
                new DefaultObjectPool<>(poolSize, () -> new FixMessage(fieldCodec), FixMessage.class, poolSize);
    }

    @Value
    public static final class Dictionary {
        private final FixSpec fixSpec;
        private final ObjectPool<FixMessage> fixMessageReadPool;
        private final ObjectPool<FixMessage> fixMessageWritePool;
    }
}
