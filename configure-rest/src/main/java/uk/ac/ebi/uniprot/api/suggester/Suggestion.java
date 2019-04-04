package uk.ac.ebi.uniprot.api.suggester;

/**
 * Created 17/12/18
 *
 * @author Edd
 */
public class Suggestion {
    private String value;
    private String id;

    @Override
    public String toString() {
        return "Suggestion{" +
                "value='" + value + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Suggestion that = (Suggestion) o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
