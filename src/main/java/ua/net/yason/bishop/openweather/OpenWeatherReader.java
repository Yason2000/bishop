package ua.net.yason.bishop.openweather;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ua.net.yason.bishop.common.reader.DefaultReader;
import ua.net.yason.bishop.common.reader.JobIntervalSupplier;
import ua.net.yason.bishop.common.reader.MeasurementValueTag;
import ua.net.yason.bishop.common.reader.ReaderValue;
import ua.net.yason.bishop.common.reader.ReaderValueConverter;
import ua.net.yason.bishop.common.reader.ReaderValueConverterSupplier;

import java.io.Closeable;
import java.io.IOException;

@Slf4j
public class OpenWeatherReader extends DefaultReader implements Closeable,
        ReaderValueConverterSupplier, JobIntervalSupplier {
    private static final String CONFIG_NODE = "openWeatherMap.";
    private final OkHttpClient client;
    private final Request request;
    private final ReaderValueConverter valueConverter;
    private final String temperatureJsonPath;
    private final String pressureJsonPath;
    private final String humidityJsonPath;

    public OpenWeatherReader(Config config) {
        super(config, CONFIG_NODE);
        client = new OkHttpClient();
        request = new Request.Builder()
                .url(config.getString(CONFIG_NODE + "queryUrl"))
                .build();
        valueConverter = new OpenWeatherValueConverter(MeasurementValueTag.builder()
                .measurement(config.getString(CONFIG_NODE + "measurement"))
                .hostName(config.getString(CONFIG_NODE + "hostName"))
                .device(config.getString(CONFIG_NODE + "device"))
                .build());
        temperatureJsonPath = config.getString(CONFIG_NODE + "temperatureJsonPath");
        pressureJsonPath = config.getString(CONFIG_NODE + "pressureJsonPath");
        humidityJsonPath = config.getString(CONFIG_NODE + "humidityJsonPath");
    }

    @Override
    public void close() {
    }

    @Override
    protected ReaderValue readValue() throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code " + response);
            }
            if (response.body() == null) {
                throw new IOException("Empty response body " + response);
            }
            DocumentContext document = JsonPath.parse(response.body().string());
            return OpenWeatherValue.builder()
                    .temperature(document.read(temperatureJsonPath, Double.class))
                    .pressure(document.read(pressureJsonPath, Double.class))
                    .humidity(document.read(humidityJsonPath, Double.class))
                    .build();
        } catch (IOException ex) {
            throw new IOException("Fail to get data from OpenWeatherMap", ex);
        }
    }

    @Override
    public ReaderValueConverter getReaderValueConverter() {
        return valueConverter;
    }
}
