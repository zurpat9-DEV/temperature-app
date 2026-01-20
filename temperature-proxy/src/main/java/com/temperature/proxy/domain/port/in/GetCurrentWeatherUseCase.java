package com.temperature.proxy.domain.port.in;

import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.WeatherData;

public interface GetCurrentWeatherUseCase {

    WeatherData getCurrentWeather(Coordinates coordinates);
}
