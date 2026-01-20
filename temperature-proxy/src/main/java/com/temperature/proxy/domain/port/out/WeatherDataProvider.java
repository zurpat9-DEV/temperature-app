package com.temperature.proxy.domain.port.out;

import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.WeatherData;

public interface WeatherDataProvider {

    WeatherData fetchWeatherData(Coordinates coordinates);
}
