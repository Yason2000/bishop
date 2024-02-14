package ua.net.yason.bishop.openweather;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;

import java.time.Instant;

@RequiredArgsConstructor
public class OpenWeatherValueConverter implements ReaderValueConverter {
    private final MeasurementValueTag valueSource;

    @Override
    public Point convert(ReaderValue value) {
        OpenWeatherValue openWeatherValue = (OpenWeatherValue) value;
        return Point
                .measurement(valueSource.getMeasurement())
                .addTag("host", valueSource.getHostName())
                .addTag("device", valueSource.getDevice())
                .addField("temperature", openWeatherValue.getTemperature())
                .addField("humidity", openWeatherValue.getHumidity())
                .addField("pressure", openWeatherValue.getPressure())
                .time(Instant.now(), WritePrecision.S);
    }
}
