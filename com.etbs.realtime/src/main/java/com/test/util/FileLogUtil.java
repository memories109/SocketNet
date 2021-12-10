package com.realtime.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.realtime.Constants;

public class FileLogUtil {
	
	private static final Logger log = LoggerFactory.getLogger(FileLogUtil.class);
	
	public static void write(String content) {
		
		try {
			
			// 저장될 파일을 생성
			String path = String.format("%s/%s", System.getProperty("user.dir"), Constants.TELEGRAM_PATH);
			
			File file = new File(path);
			
			if(!file.exists()) {
				file.mkdir();
			}
			
			String fileName = String.format("%s/%s/%s.log", System.getProperty("user.dir"), Constants.TELEGRAM_PATH, DateUtil.getCurrentDateString());
			
			file = new File(fileName);
			
			if(!file.exists()) {
				file.createNewFile();
			}	
			
			try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true))) {

				out.write(String.format("[%s] %s", DateUtil.getCurrentDateTimeString(),content));
				out.newLine();

				out.close();
			}	      
	    }
		catch (IOException e) {
			log.error("전문쓰기 ERROR >> "+e.getMessage());
			e.printStackTrace();
	    }
	}
	
}