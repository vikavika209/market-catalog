package com.pet.loggingspringbootstarter.logging;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(LoggingConfiguration.class)
public @interface EnableLogging {
}
