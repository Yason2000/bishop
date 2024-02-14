package ua.net.yason.bishop.sht31;

import lombok.Builder;
import lombok.Data;
import ua.net.yason.bishop.common.Utils;
import ua.net.yason.bishop.common.reader.ReaderValue;

@Data
@Builder
class Sht31Value implements ReaderValue {
    private final double temperature;
    private final double humidity;

    @Override
    public String toString() {
        return "Sht31Value(temperature=" + Utils.toString(this.getTemperature())
                + ", humidity=" + Utils.toString(this.getHumidity())
                + ")";
    }
}
