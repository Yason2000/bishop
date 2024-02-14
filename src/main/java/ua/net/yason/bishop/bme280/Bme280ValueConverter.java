package ua.net.yason.bishop.bme280;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;

import java.time.Instant;

@RequiredArgsConstructor
class Bme280ValueConverter implements ReaderValueConverter {
    private final MeasurementValueTag valueSource;

    @Override
    public Point convert(ReaderValue value) {
        Bme280Value bme280Value = (Bme280Value) value;
        return Point
                .measurement(valueSource.getMeasurement())
                .addTag("host", valueSource.getHostName())
                .addTag("device", valueSource.getDevice())
                .addField("temperature", bme280Value.getTemperature())
                .addField("pressure", bme280Value.getPressure())
                .addField("humidity", bme280Value.getHumidity())
                .time(Instant.now(), WritePrecision.S);
    }
}
