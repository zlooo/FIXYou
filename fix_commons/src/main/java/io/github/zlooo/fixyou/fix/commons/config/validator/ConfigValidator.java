package io.github.zlooo.fixyou.fix.commons.config.validator;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.session.SessionConfig;

import java.util.Set;

public interface ConfigValidator {

    Set<String> validateConfig(FIXYouConfiguration fixYouConfiguration);

    Set<String> validateSessionConfig(SessionConfig sessionConfig);
}
