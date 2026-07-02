package com.slowloris.monitor.entity;

import lombok.Data;

@Data
public class SuspiciousIp {
    private String ipAddress;
    private String country;
    private String countryCode;
    private String city;
    private String isp;

    public static SuspiciousIp unknown(String ip) {
        SuspiciousIp s = new SuspiciousIp();
        s.setIpAddress(ip);
        s.setCountry("未知");
        s.setCountryCode("--");
        s.setCity("未知");
        s.setIsp("未知");
        return s;
    }
}
