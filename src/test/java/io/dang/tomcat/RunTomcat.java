package io.dang.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import java.io.File;

/**
 * User: jcdang Date: 7/10/15
 */

public class RunTomcat {

    public static void main(String[] args) throws ServletException, LifecycleException {

        final int TOMCAT_PORT = 8080;
        String webAppPath = "src/test/webapp";
        final Tomcat tomcat = new Tomcat();
        tomcat.setPort(TOMCAT_PORT);
        tomcat.setSilent(true);
        tomcat.addWebapp("", new File(webAppPath).getAbsolutePath());
        tomcat.start();



        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    tomcat.stop();
                } catch (LifecycleException ignored) {
                }
            }
        });
    }
}
