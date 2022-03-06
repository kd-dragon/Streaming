package com.kdy.bean.util.media.h264;

public class H264CodecConfigInfo {

	public int    profileIDC                  = 0;
	public int    levelIDC                    = 0;
	public int    spsID                       = 0;
	public int    chromaFormatIDC             = 0;
	public int    residualColorTransformFlag  = 0;
	public int    bitDepthLumaMinus8          = 0;
	public int    bitDepthChromaMinus8        = 0;
	public int    transformBypass             = 0;
	public int    scalingMatrixFlag           = 0;
	public int    log2MaxFrameNum             = 0;
	public int    pocType                     = 0;
	public int    log2MaxPocLSB               = 0;
	public int    deltaPicOrderAlwaysZeroFlag = 0;
	public int    offsetForNonRefPic          = 0;
	public int    offsetForTopToBottomField   = 0;
	public int    pocCycleLength              = 0;
	public int[]  offsetForRefFrame           = null;
	public int    refFrameCount               = 0;
	public int    gapsInFrameNumAllowedFlag   = 0;
	public int    mbWidth                     = 0;
	public int    mbHeight                    = 0;
	public int    frameMBSOnlyFlag            = 0;
	public int    mbAFF                       = 0;
	public int    adjWidth                    = 0;
	public int    adjHeight                   = 0;
	public int    direct8x8InferenceFlag      = 0;
	public int    crop                        = 0;
	public int    cropLeft                    = 0;
	public int    cropRight                   = 0;
	public int    cropTop                     = 0;
	public int    cropBottom                  = 0;
	public int    vuiParametersPresentFlag    = 0;
	public int    videoSignalTypePresentFlag  = 0;
	public int    videoFormat                 = 0;
	public int    videoFullRange              = 0;
	public long   timingNumUnitsInTick        = 0L;
	public long   timingTimescale             = 0L;
	public int    timingFixedFrameRateFlag    = 0;
	public double frameRate                   = 0.0D;
	public int    sarNum                      = 0;
	public int    sarDen                      = 0;
	public int    aspectRatioIDC              = 0;
	public int    aspectRatioInfoPresentFlag  = 0;
	public int    height                      = 0;
	public int    width                       = 0;
	public int    displayHeight               = 0;
	public int    displayWidth                = 0;

}
