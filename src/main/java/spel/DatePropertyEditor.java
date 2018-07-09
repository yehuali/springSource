package spel;


import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatePropertyEditor extends PropertyEditorSupport {
    private String format = "yyyy-MM-dd";

    public String getFormat() {
        return format;
    }

    public void setFormat(String format){
        this.format = format;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        System.out.println("text:"+text);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try{
            Date d = sdf.parse(text);
            this.setValue(d);
        }catch (ParseException e){
            e.printStackTrace();
        }
    }
}
