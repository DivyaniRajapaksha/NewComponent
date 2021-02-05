package com.gateway;

import com.gateway.listeners.JMSEventListener;

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.io.IOException;

public class MainClass {
    public static void main(String[] args) throws InterruptedException, IOException, JMSException, NamingException {

        JMSEventListener jmsEventListener = new JMSEventListener();
        jmsEventListener.setSubscriber();
    }

}
