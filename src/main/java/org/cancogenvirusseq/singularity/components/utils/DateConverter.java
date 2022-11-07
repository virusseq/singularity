package org.cancogenvirusseq.singularity.components.utils;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cancogenvirusseq.singularity.config.utils.UtilsProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateConverter {

    private final UtilsProperties utilsProperties;

    public ZonedDateTime instantToZonedDateTime(Instant instant){
        try {
            return instant.atZone(ZoneId.of(utilsProperties.getTimezone()));
        }catch (Exception e){
            return instant.atZone(ZoneId.systemDefault());
        }
    }
}
