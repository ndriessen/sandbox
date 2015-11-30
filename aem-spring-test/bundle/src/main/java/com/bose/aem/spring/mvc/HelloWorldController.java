package com.bose.aem.spring.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HelloWorldController {
    @Autowired
    private String someOtherBean;
    @Autowired
    private String greetingMessage;

    @RequestMapping("/echo/{param}")
    @ResponseBody
    public String echo(@PathVariable("param") String paramToEcho) {
        return String.format("%s %s (%s)", greetingMessage, paramToEcho,someOtherBean);
    }
}
