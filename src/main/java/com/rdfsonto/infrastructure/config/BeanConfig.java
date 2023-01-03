package com.rdfsonto.infrastructure.config;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import net.minidev.json.parser.JSONParser;


@Configuration
public class BeanConfig
{
    @Bean
    RestTemplate restTemplate()
    {
        return new RestTemplate();
    }

    @Bean
    JSONParser jsonParser()
    {
        return new JSONParser(JSONParser.MODE_JSON_SIMPLE);
    }

    @Bean
    EmailValidator emailValidator()
    {
        return EmailValidator.getInstance();
    }
}
