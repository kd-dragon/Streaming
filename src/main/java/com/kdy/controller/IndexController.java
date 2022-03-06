package com.kdy.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
	
	private final static Logger log = LoggerFactory.getLogger(IndexController.class);
	
	@GetMapping(value="/")
	public ModelAndView home() throws Exception {
		log.info("Stream Server is Running  ...");
		
		ModelAndView modelAndView = new ModelAndView();
		Map<String, Object> map = new HashMap<>();
		map.put("name", "tigensoft");
		map.put("date", LocalDateTime.now());
		
		modelAndView.addObject("data", map);
		modelAndView.setViewName("index");
		
		return modelAndView;
	}
}
