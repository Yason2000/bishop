package ua.net.yason.bishop.mhz19c;

import lombok.Builder;
import lombok.Data;
import ua.net.yason.bishop.common.Utils;
import ua.net.yason.bishop.common.reader.ReaderValue;

@Data
@Builder
class Mhz19cValue implements ReaderValue {
    private final double co2;
    private final double temperature;

    @Override
    public String toString() {
        return "Mhz19cValue(temperature=" + Utils.toString(this.getTemperature())
                + ", co2=" + Utils.toString(this.getCo2())
                + ")";
    }
}
