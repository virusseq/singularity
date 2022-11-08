package org.cancogenvirusseq.singularity.components.notifications.archives;

import lombok.AllArgsConstructor;
import lombok.val;
import org.cancogenvirusseq.singularity.repository.model.ArchiveStatus;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;

@AllArgsConstructor
public class Message {
    ArchiveStatus status;
    String hash;
    ZonedDateTime createdAt;

    public LinkedHashMap<String, ? extends Object> toLinkedHashMap(){
        val newHashMap = new LinkedHashMap<String, Object>();
        newHashMap.put("Status", status);
        newHashMap.put("Hash", hash);
        newHashMap.put("Created at", createdAt);
        return newHashMap;
    }

}
