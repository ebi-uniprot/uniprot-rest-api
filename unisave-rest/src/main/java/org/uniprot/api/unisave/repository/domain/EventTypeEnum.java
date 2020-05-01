package org.uniprot.api.unisave.repository.domain;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:20 To change this template use
 * File | Settings | File Templates.
 */
public enum EventTypeEnum {
    MERGED("merged"),
    REPLACING("replacing"),
    DELETED("deleted");

    private final String name;

    EventTypeEnum(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static EventTypeEnum fromEventType(String name) {
        switch (name) {
            case "merged":
                return MERGED;
            case "replacing":
                return REPLACING;
            case "deleted":
                return DELETED;
            default:
                throw new IllegalArgumentException(
                        "The supplied name, '" + name + "', does not exist.");
        }
    }
}
