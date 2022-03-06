package com.kdy.bean.monitor;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id="system.dist.usage")
public class CustomDiskUsage {

	@ReadOperation
    public Map<String, Object> diskUsage() {
		
        Map<String, Object> details = new LinkedHashMap<>();
        
        File root = new File(System.getProperty("user.dir"));
		long total = root.getTotalSpace();
		long useable = root.getUsableSpace();
		
		details.put("value", Long.toString(total-useable));
        
        return details;
    }
	
}
