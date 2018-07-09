package spel;

import java.util.Date;

public class UserManager {
    private Date dataValue;

    public Date getDataValue() {
        return dataValue;
    }

    public void setDataValue(Date dataValue) {
        this.dataValue = dataValue;
    }

    @Override
    public String toString() {
        return "UserManager{" +
                "dataValue=" + dataValue +
                '}';
    }
}
