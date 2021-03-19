package com.github.frunoyman.shell;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppiumShell extends Shell {
    private Logger logger;
    private AndroidDriver driver;
    private String[] permissions = new String[]{
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };

    public AppiumShell(AndroidDriver driver) {
        this.driver = driver;
        logger = Logger.getLogger(AppiumShell.class.getName() + "] [" + getSerial());

    }

    @Override
    public String execute(String... command) throws Exception {
        StringBuilder commandBuilder = new StringBuilder();
        for (String var : command) {
            commandBuilder.append(var);
            commandBuilder.append(" ");
        }
        Map<String, Object> args = new HashMap<>();
        args.put("command", commandBuilder.toString());
        return driver.executeScript("mobile: shell", args).toString();
    }

    @Override
    public String executeBroadcast(String... command) throws Exception {
        if (!execute("pm list packages -3").contains(REMOTE_PACKAGE)){
            throw new Exception("Pls install RemoteController apk");
        }
        if(!execute("ps -A").contains(REMOTE_PACKAGE)){
            logger.debug("Remote controller was not running, starting ...");
            execute("am", "start", "-n", REMOTE_PACKAGE + "/.MainActivity");
            Thread.sleep(3000);
        }
        StringBuilder commandBuilder = new StringBuilder();
        for (String var : command) {
            commandBuilder.append(var);
            commandBuilder.append(" ");
        }
        Map<String, Object> args = new HashMap<>();
        args.put("command", commandBuilder.toString());
        String output = driver.executeScript("mobile: shell", args).toString();
        Pattern r = Pattern.compile(ADAPTER_PATTERN);
        Matcher m = r.matcher(output);
        if (m.matches()) {
            if (output.contains("result=" + ERROR_CODE)) {
                ObjectMapper objectMapper = new ObjectMapper();
                Exception exception = objectMapper.readValue(m.group(3), Exception.class);
                throw exception;
            } else {
                return m.group(3);
            }
        }else if (output.contains("result="+EMPTY_BROADCAST_CODE)){
            throw new Exception("Empty broadcast");
        }
        return output;
    }

    @Override
    public String getSerial() {
        return (String) driver.getCapabilities().getCapability(MobileCapabilityType.UDID);
    }
}
