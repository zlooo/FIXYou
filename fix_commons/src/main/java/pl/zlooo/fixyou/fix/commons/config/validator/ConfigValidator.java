package pl.zlooo.fixyou.fix.commons.config.validator;

import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.session.SessionConfig;

import java.util.Set;

public interface ConfigValidator {

    Set<String> validateConfig(FIXYouConfiguration fixYouConfiguration);

    Set<String> validateSessionConfig(SessionConfig sessionConfig);
}
