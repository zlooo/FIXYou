package pl.zlooo.fixyou.fix.commons;

import lombok.Value;
import pl.zlooo.fixyou.DefaultConfiguration;
import pl.zlooo.fixyou.FIXYouException;
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool;
import pl.zlooo.fixyou.model.FixSpec;
import pl.zlooo.fixyou.parser.model.FixMessage;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DictionaryRepository {

    private Map<String, Dictionary> dictionaries = new HashMap<>();

    @Inject
    DictionaryRepository() {
    }

    public @Nullable
    Dictionary getDictionary(String dictionaryID) {
        return dictionaries.get(dictionaryID);
    }

    public void registerDictionary(String dictionaryID, FixSpec fixSpec) {
        if (dictionaries.putIfAbsent(dictionaryID,
                                     new Dictionary(fixSpec, new DefaultObjectPool<>(DefaultConfiguration.FIX_MESSAGE_POOL_SIZE, () -> new FixMessage(fixSpec), FixMessage.class))) !=
            null) {
            throw new FIXYouException("Dictionary with id " + dictionaryID + " already exist");
        }
    }

    @Value
    public static final class Dictionary {
        private final FixSpec fixSpec;
        private final DefaultObjectPool<FixMessage> fixMessageObjectPool;
    }
}
