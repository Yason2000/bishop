package ua.net.yason.bishop.sht31;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;

import java.time.Instant;

@RequiredArgsConstructor
class Sht31ValueConverter implements ReaderValueConverter {
    private final MeasurementValueTag valueSource;

    @Override
    public Point convert(ReaderValue value) {
        Sht31Value sht31Value = (Sht31Value) value;
        return Point
                .measurement(valueSource.getMeasurement())
                .addTag("host", valueSource.getHostName())
                .addTag("device", valueSource.getDevice())
                .addField("temperature", sht31Value.getTemperature())
                .addField("humidity", sht31Value.getHumidity())
                .time(Instant.now(), WritePrecision.S);
    }
}
