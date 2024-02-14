package ua.net.yason.bishop.common.reader;

import com.influxdb.client.write.Point;

public interface ReaderValueConverter {
    Point convert(ReaderValue value);
}
