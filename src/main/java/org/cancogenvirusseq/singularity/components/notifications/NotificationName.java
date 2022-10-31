package org.cancogenvirusseq.singularity.components.notifications;

import lombok.Getter;

@Getter
public enum NotificationName {
    ALL("", null),
    BUILDING_RELEASE("Release status", NotificationCategory.INFO),
    COMPLETE_RELEASE("Release status", NotificationCategory.INFO),
    FAILED_RELEASE("Release status", NotificationCategory.ERROR),
    CANCELLED_RELEASE("Release status", NotificationCategory.INFO),
    UNHANDLED_ERROR("Release status", NotificationCategory.ERROR);

    private final NotificationCategory category;
    private final String header;

    NotificationName(String header, NotificationCategory category) {
        this.header = header;
        this.category = category;
    }
}
