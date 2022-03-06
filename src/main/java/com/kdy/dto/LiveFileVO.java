package com.kdy.dto;

import java.io.Serializable;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class LiveFileVO implements Serializable {
	private String liveFileSeq;			//live File Seq
	private String liveFullFilePath;	//live Path + live name
	private String liveFileName;		//live name
	private String liveQuality;			// mid, high
}
