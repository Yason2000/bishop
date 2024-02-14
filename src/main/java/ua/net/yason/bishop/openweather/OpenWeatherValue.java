package ua.net.yason.bishop.openweather;

import lombok.Builder;
import lombok.Data;
import ua.net.yason.bishop.common.Utils;
import ua.net.yason.bishop.common.reader.ReaderValue;

@Data
@Builder
class OpenWeatherValue implements ReaderValue {
    private final double temperature;
    private final double humidity;
    private final double pressure;

    @Override
    public String toString() {
        return "OpenWeatherValue(temperature=" + Utils.toString(this.getTemperature())
                + ", humidity=" + Utils.toString(this.getHumidity())
                + ", pressure=" + Utils.toString(this.getPressure())
                + ")";
    }
}
