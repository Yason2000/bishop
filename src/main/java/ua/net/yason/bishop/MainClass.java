package ua.net.yason.bishop;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import ua.net.yason.bishop.bme280.Bme280Reader;
import ua.net.yason.bishop.common.integration.Configuration;
import ua.net.yason.bishop.common.integration.InfluxDb;
import ua.net.yason.bishop.common.integration.Quartz;
import ua.net.yason.bishop.common.integration.RaspberryPi;
import ua.net.yason.bishop.common.reader.ReaderJobScheduler;
import ua.net.yason.bishop.f5000.F5000Reader;
import ua.net.yason.bishop.mhz19c.Mhz19cReader;
import ua.net.yason.bishop.openweather.OpenWeatherReader;
import ua.net.yason.bishop.sht31.Sht31Reader;

@Slf4j
public class MainClass {
    public static void main(String[] args) {
        Config config = Configuration.getConfig();
        try (
                Quartz quartz = new Quartz();
                InfluxDb db = new InfluxDb(config);
                RaspberryPi pi = new RaspberryPi();
                Sht31Reader sht31 = new Sht31Reader(config, pi);
                Bme280Reader bme280 = new Bme280Reader(config, pi);
                Mhz19cReader mhz19c = new Mhz19cReader(config, pi);
                F5000Reader f5000 = new F5000Reader(config);
                OpenWeatherReader openWeather = new OpenWeatherReader(config);
        ) {
            ReaderJobScheduler jobScheduler = new ReaderJobScheduler(quartz, db);
            jobScheduler.schedule(openWeather, openWeather, openWeather);
            jobScheduler.schedule(sht31, sht31, sht31);
            jobScheduler.schedule(bme280, bme280, bme280);
            jobScheduler.schedule(mhz19c, mhz19c, mhz19c);
            jobScheduler.schedule(f5000, f5000, f5000);
            infiniteLoop();
        } catch (Exception ex) {
            log.error("Application fatal error", ex);
        }
    }

    private static void infiniteLoop() throws InterruptedException {
        while (true) {
            Thread.sleep(10000L);
            log.debug("heartbeat");
        }
    }
}
