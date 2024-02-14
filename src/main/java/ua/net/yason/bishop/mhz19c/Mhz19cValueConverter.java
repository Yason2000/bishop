package ua.net.yason.bishop.mhz19c;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;

import java.time.Instant;

@RequiredArgsConstructor
class Mhz19cValueConverter implements ReaderValueConverter {
    private final MeasurementValueTag valueSource;

    @Override
    public Point convert(ReaderValue value) {
        Mhz19cValue mhz19cValue = (Mhz19cValue) value;
        return Point
                .measurement(valueSource.getMeasurement())
                .addTag("host", valueSource.getHostName())
                .addTag("device", valueSource.getDevice())
                .addField("temperature", mhz19cValue.getTemperature())
                .addField("co2", mhz19cValue.getCo2())
                .time(Instant.now(), WritePrecision.S);
    }
}
