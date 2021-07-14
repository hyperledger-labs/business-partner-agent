package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.Factory;

import javax.inject.Singleton;
import java.time.Clock;

@Factory
public class ClockFactory {

    @Singleton
    Clock systemClock(){
        return Clock.systemUTC();
    }
}
