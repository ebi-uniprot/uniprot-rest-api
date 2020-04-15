package org.uniprot.api.unisave.repository.domain.impl;

import java.io.Serializable;

import org.uniprot.api.unisave.repository.domain.EventTypeEnum;

public class IdentifierStatusId implements Serializable {

    private EventTypeEnum getType;

    private String firstColumn;

    private String secondColumn;

    public EventTypeEnum getGetType() {
        return getType;
    }

    public void setGetType(EventTypeEnum getType) {
        this.getType = getType;
    }

    public String getFirstColumn() {
        return firstColumn;
    }

    public void setFirstColumn(String firstColumn) {
        this.firstColumn = firstColumn;
    }

    public String getSecondColumn() {
        return secondColumn;
    }

    public void setSecondColumn(String secondColumn) {
        this.secondColumn = secondColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentifierStatusId that = (IdentifierStatusId) o;

        if (!firstColumn.equals(that.firstColumn)) return false;
        if (getType != that.getType) return false;
        if (secondColumn != null
                ? !secondColumn.equals(that.secondColumn)
                : that.secondColumn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getType.hashCode();
        result = 31 * result + firstColumn.hashCode();
        result = 31 * result + (secondColumn != null ? secondColumn.hashCode() : 0);
        return result;
    }
}
