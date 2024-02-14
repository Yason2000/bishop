package ua.net.yason.bishop.common.reader;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeasurementValueTag {
    private final String measurement;
    private final String hostName;
    private final String device;
}
