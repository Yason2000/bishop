package ua.net.yason.bishop.common.reader;

import java.io.IOException;

public interface Reader {
    boolean isEnable();
    ReaderValue read() throws IOException;
}
