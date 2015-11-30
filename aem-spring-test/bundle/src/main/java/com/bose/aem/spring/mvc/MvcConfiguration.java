package com.bose.aem.spring.mvc;

import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * Created by niki on 27/11/15.
 */
//@Configuration
public class MvcConfiguration {

    public MvcConfiguration() {
        //dirty hack to force import
        ReflectUtils.class.getName();
    }

    @Bean
    public String someOtherBean(){
        return "Some other bean";
    }
}
