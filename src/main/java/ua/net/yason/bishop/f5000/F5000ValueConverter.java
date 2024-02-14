package ua.net.yason.bishop.f5000;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;

import java.time.Instant;

@RequiredArgsConstructor
class F5000ValueConverter implements ReaderValueConverter {
    private final MeasurementValueTag valueSource;

    @Override
    public Point convert(ReaderValue value) {
        F5000Value mhz19cValue = (F5000Value) value;
        return Point
                .measurement(valueSource.getMeasurement())
                .addTag("host", valueSource.getHostName())
                .addTag("device", valueSource.getDevice())
                .addField("lastSievert", mhz19cValue.getLastSievert())
                .addField("averageSievert", mhz19cValue.getAverageSievert())
                .addField("totalSievert", mhz19cValue.getTotalSievert())
                .time(Instant.now(), WritePrecision.S);
    }
}
