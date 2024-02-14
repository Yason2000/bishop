package ua.net.yason.bishop.f5000;

import lombok.Builder;
import lombok.Data;
import ua.net.yason.bishop.common.Utils;
import ua.net.yason.bishop.common.reader.ReaderValue;

@Data
@Builder
class F5000Value implements ReaderValue {
    private final double lastSievert;
    private final double averageSievert;
    private final double totalSievert;

    @Override
    public String toString() {
        return "F5000Value(lastSievert=" + Utils.toString(this.getLastSievert())
                + ", averageSievert=" + Utils.toString(this.getAverageSievert())
                + ", totalSievert=" + Utils.toString(this.getTotalSievert())
                + ")";
    }
}
