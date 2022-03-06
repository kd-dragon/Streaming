package com.kdy.dto;

import java.io.Serializable;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class VodFileVO implements Serializable {
	private String vodFileSeq;  // vod File Seq
	private String vodFullFilePath; // vod path + vod name
	private String vodFileName; // vod name
	private String vodQuality; // low, mid, high
}
