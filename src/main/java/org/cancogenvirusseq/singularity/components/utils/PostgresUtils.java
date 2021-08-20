package org.cancogenvirusseq.singularity.components.utils;

import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import io.r2dbc.spi.R2dbcException;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.dao.DataIntegrityViolationException;

public class PostgresUtils {
  public static Optional<String> getSqlStateOptionalFromException(
      DataIntegrityViolationException dataViolation) {
    return Optional.ofNullable(
            ((R2dbcDataIntegrityViolationException) dataViolation.getRootCause()))
        .map(R2dbcException::getSqlState);
  }

  public static Predicate<String> isUniqueViolationError = errorCode -> errorCode.equals("23505");
}
