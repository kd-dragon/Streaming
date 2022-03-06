package com.kdy.bean.util.io.mp4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.AbstractMediaHeaderBox;
import com.coremedia.iso.boxes.ChunkOffset64BitBox;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SampleToChunkBox.Entry;
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.apple.AppleWaveBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderConfigDescriptor;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.DecoderSpecificInfo;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ESDescriptor;
import com.mp4parser.iso14496.part15.AvcConfigurationBox;
import com.mp4parser.iso14496.part15.AvcDecoderConfigurationRecord;
import com.kdy.bean.util.io.Tag;
import com.kdy.bean.util.io.Interface.IKeyFrameDataAnalyzer;
import com.kdy.bean.util.io.Interface.ITag;
import com.kdy.bean.util.io.Interface.ITagReader;
import com.kdy.bean.util.io.Interface.IoConstants;
import com.kdy.bean.util.io.amf.Output;
import com.kdy.bean.util.stream.IF.IStreamableFile;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class MP4Reader implements Serializable, IoConstants, ITagReader, IKeyFrameDataAnalyzer {
	
	private final static Logger log = LoggerFactory.getLogger(MP4Reader.class);
	
	/** Audio packet prefix for the decoder frame */
    public final static byte[] PREFIX_AUDIO_CONFIG_FRAME = new byte[] { (byte) 0xaf, (byte) 0 };
    
	/** Audio packet prefix */
	public final static byte[] PREFIX_AUDIO_FRAME = new byte[] { (byte) 0xaf, (byte) 0x01 };

	/** Blank AAC data **/
	public final static byte[] EMPTY_AAC = { (byte) 0x21, (byte) 0x10, (byte) 0x04, (byte) 0x60, (byte) 0x8c, (byte) 0x1c };
	
	/** Audio config aac main */
	public final static byte[] AUDIO_CONFIG_FRAME_AAC_MAIN = new byte[] { (byte) 0x0a, (byte) 0x10 };

	/** Audio config aac lc */
	public final static byte[] AUDIO_CONFIG_FRAME_AAC_LC = new byte[] { (byte) 0x12, (byte) 0x10 };

	/** Audio config sbr */
	public final static byte[] AUDIO_CONFIG_FRAME_SBR = new byte[] { (byte) 0x13, (byte) 0x90, (byte) 0x56, (byte) 0xe5, (byte) 0xa5, (byte) 0x48, (byte) 0x00 };

	/** Video packet prefix for the decoder frame */
	public final static byte[] PREFIX_VIDEO_CONFIG_FRAME = new byte[] { (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	/** Video packet prefix for key frames */
	public final static byte[] PREFIX_VIDEO_KEYFRAME = new byte[] { (byte) 0x17, (byte) 0x01 };

	/** Video packet prefix for standard frames (interframe) */
	public final static byte[] PREFIX_VIDEO_FRAME = new byte[] { (byte) 0x27, (byte) 0x01 };
	
	/**
	 * File
	 */
	private File file;

	/**
	 * Input stream
	 */
	private FileInputStream fis;

	/**
	 * File channel
	 */
	private SeekableByteChannel channel;
	
	/**
	 * Provider of boxes
	 */
	private IsoFile isoFile;

	/** Mapping between file position and timestamp in ms. */
	private HashMap<Integer, Long> timePosMap;

	private HashMap<Integer, Long> samplePosMap;

	/** Whether or not the clip contains a video track */
	private boolean hasVideo = false;

	/** Whether or not the clip contains an audio track */
	private boolean hasAudio = false;

	//default video codec 
	private String videoCodecId = "avc1";

	//default audio codec 
	private String audioCodecId = "mp4a";

	//decoder bytes / configs
	private byte[] audioDecoderBytes;

	private byte[] videoDecoderBytes;

	// duration in milliseconds
	private long duration;

	// movie time scale
	private long timeScale;

	private int width;

	private int height;

	//audio sample rate kHz
	private double audioTimeScale;

	private int audioChannels;

	//default to aac lc
	private int audioCodecType = 1;

	private long videoSampleCount;

	private double fps;

	private double videoTimeScale;

	private int avcLevel;

	private int avcProfile;

	private String formattedDuration;

	//private long moovOffset;

	private long mdatOffset;

	//samples to chunk mappings
	private List<SampleToChunkBox.Entry> videoSamplesToChunks;

	private List<SampleToChunkBox.Entry> audioSamplesToChunks;

	//keyframe - sample numbers
	private long[] syncSamples;

	//samples 
	private long[] videoSamples;

	private long[] audioSamples;
	
	private long audioSampleSize;

	//chunk offsets
	private long[] videoChunkOffsets;

	private long[] audioChunkOffsets;

	//sample duration
	private long videoSampleDuration = 125;

	private long audioSampleDuration = 1024;

	//keep track of current frame / sample
	private int currentFrame = 0;

	private int prevFrameSize = 0;

	private int prevVideoTS = -1;
	
	private List<MP4Frame> frames = new ArrayList<MP4Frame>();

	private long audioCount;

	private long videoCount;

	// composition time to sample entries
	private List<CompositionTimeToSample.Entry> compositionTimes;

	/**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
	private LinkedList<ITag> firstTags = new LinkedList<ITag>();

	/**
	 * Container for seek points in the video. These are the time stamps
	 * for the key frames.
	 */
	private LinkedList<Integer> seekPoints;

	/** Constructs a new MP4Reader. */
	MP4Reader() {
	}

	/**
	 * Creates MP4 reader from file input stream, sets up metadata generation flag.
	 *
	 * @param f                    File input stream
	 */
	public MP4Reader(File f) throws IOException {
		
		/**
		if (null == f) {
			log.warn("Reader was passed a null file");
			log.debug("{}", ToStringBuilder.reflectionToString(this));
		}
		this.file = f;
		if (file.exists() && file.canRead()) {
			this.fis = new FileInputStream(f);
			channel = fis.getChannel();
			// instance an iso file from mp4parser
			isoFile = new IsoFile(channel);
			//decode all the info that we want from the atoms
			decodeHeader();
			//analyze the samples/chunks and build the keyframe meta data
			analyzeFrames();
			//add meta data
			firstTags.add(createFileMeta());
			//create / add the pre-streaming (decoder config) tags
			createPreStreamingTags(0, false);
		} else {
			log.warn("Reader was passed an unreadable or non-existant file");
		}
		*/
		
		this.file = f;
		//this.fis = new FileInputStream(f);
		//channel = fis.getChannel();
		channel = Files.newByteChannel(Paths.get(f.toURI()));
		isoFile = new IsoFile(f.getAbsolutePath());
		
		decodeHeader();
		analyzeFrames();
		firstTags.add(createFileMeta());
		createPreStreamingTags(0, false);
	}
	
	@Override
	public List<MP4Frame> getAnalyzeFrames() {
		return analyzeFrames();
	}
	
	@Override
	public void setFrames(List<MP4Frame> frames) {
		this.frames = frames;
	}
	
	@Override
	public void setStreamingTags() {
		createPreStreamingTags(0, false);
	}

	@Override
	public void initFirstTags() {
		firstTags.add(createFileMeta());
	}
	
	@Override
	public void setVodFileChannel(File f) {
		try {
			this.file = f;
			//this.fis = new FileInputStream(f);
			//this.channel = fis.getChannel();
			if(f.exists() && f.canRead()) {
				this.channel = Files.newByteChannel(Paths.get(f.toURI()));
			} else {
				log.warn("### Reader was passed an unreadable or non-existant file ###");
			}
		} catch(FileNotFoundException e) {
			log.error("MP4Reader - File Not Found ");
		} catch(IOException ioe) {
			log.error("Error : {}", ioe);
		}
	}
	

	/**
	 * This handles the moov atom being at the beginning or end of the file, so the mdat may also
	 * be before or after the moov atom.
	 */
	public void decodeHeader() {
		
		try {
			 // we want a moov and an mdat, anything else will throw the invalid file type error
	        MovieBox moov = isoFile.getBoxes(MovieBox.class).get(0);
	        // get the movie header
	        MovieHeaderBox mvhd = moov.getMovieHeaderBox();
	        // get the timescale and duration
	        timeScale = mvhd.getTimescale();
	        duration = mvhd.getDuration();
	        // look at the tracks
	        List<TrackBox> tracks = moov.getBoxes(TrackBox.class); // trak
	        for (TrackBox trak : tracks) {
	            TrackHeaderBox tkhd = trak.getTrackHeaderBox(); // tkhd
	            if (tkhd != null && tkhd.getWidth() > 0) {
	                width = (int) tkhd.getWidth();
	                height = (int) tkhd.getHeight();
	            }
	            MediaBox mdia = trak.getMediaBox(); // mdia
	            long scale = 0;
	            boolean isAudio = false, isVideo = false;
	            if (mdia != null) {
	                MediaHeaderBox mdhd = mdia.getMediaHeaderBox(); // mdhd
	                if (mdhd != null) {
	                    // this will be for either video or audio depending media info
	                    scale = mdhd.getTimescale();
	                }
	                HandlerBox hdlr = mdia.getHandlerBox(); // hdlr
	                if (hdlr != null) {
	                    String hdlrType = hdlr.getHandlerType();
	                    if ("vide".equals(hdlrType)) {
	                        hasVideo = true;
	                        if (scale > 0) {
	                            videoTimeScale = scale * 1.0;
	                        }
	                    } else if ("soun".equals(hdlrType)) {
	                        hasAudio = true;
	                        if (scale > 0) {
	                            audioTimeScale = scale * 1.0;
	                        }
	                    } else {
	                        log.info("Unhandled handler type: {}", hdlrType);
	                    }
	                }
	                MediaInformationBox minf = mdia.getMediaInformationBox();
	                if (minf != null) {
	                    AbstractMediaHeaderBox abs = minf.getMediaHeaderBox();
	                    if (abs != null) {
	                        if (abs instanceof SoundMediaHeaderBox) { // smhd
	                            //SoundMediaHeaderBox smhd = (SoundMediaHeaderBox) abs;
	                            isAudio = true;
	                        } else if (abs instanceof VideoMediaHeaderBox) { // vmhd
	                            //VideoMediaHeaderBox vmhd = (VideoMediaHeaderBox) abs;
	                            isVideo = true;
	                        } else {
	                            log.warn("Unhandled media header box: {}", abs.getType());
	                        }
	                    } else {
	                        log.warn("Null media header box");
	                    }
	                }
	            }
	            SampleTableBox stbl = trak.getSampleTableBox(); // mdia/minf/stbl
	            if (stbl != null) {
	                SampleDescriptionBox stsd = stbl.getSampleDescriptionBox(); // stsd
	                if (stsd != null) {
	                    //stsd: mp4a, avc1, mp4v
	                    //String type = stsd.getType();
	                    SampleEntry entry = stsd.getSampleEntry();
	                    if (entry != null) {
	                        // determine if audio or video and process from there
	                        if (entry instanceof AudioSampleEntry) {
	                            processAudioBox(stbl, (AudioSampleEntry) entry, scale);
	                        } else if (entry instanceof VisualSampleEntry) {
	                            processVideoBox(stbl, (VisualSampleEntry) entry, scale);
	                        }
	                    } else {
	                        if (isVideo) {
	                            processVideoBox(stbl, scale);
	                        } else if (isAudio) {
	                            processAudioBox(stbl, scale);
	                        }
	                    }
	                }
	            }
	        }
	        //calculate FPS
	        fps = (videoSampleCount * timeScale) / (double) duration;
	        //real duration
	        StringBuilder sb = new StringBuilder();
	        double videoTime = ((double) duration / (double) timeScale);
	        int minutes = (int) (videoTime / 60);
	        if (minutes > 0) {
	            sb.append(minutes);
	            sb.append('.');
	        }
	        //formatter for seconds / millis
	        NumberFormat df = DecimalFormat.getInstance();
	        df.setMaximumFractionDigits(2);
	        sb.append(df.format((videoTime % 60)));
	        formattedDuration = sb.toString();
	
	        // detect whether or not this movie contains fragments first
	        List<MovieFragmentBox> moofs = isoFile.getBoxes(MovieFragmentBox.class); // moof
	        if (moofs != null && !moofs.isEmpty()) {
	            for (MovieFragmentBox moof : moofs) {
	                //List<Long> syncSamples = moof.getSyncSamples(sdtp);
	                if (compositionTimes == null) {
	                    compositionTimes = new ArrayList<>();
	                }
	                LinkedList<Integer> dataOffsets = new LinkedList<>();
	                LinkedList<Long> sampleSizes = new LinkedList<>();
	                List<TrackRunBox> truns = moof.getTrackRunBoxes();
	                for (TrackRunBox trun : truns) {
	                    //videoSamplesToChunks
	                    if (trun.isDataOffsetPresent()) {
	                        dataOffsets.add(trun.getDataOffset());
	                    }
	                    videoSampleCount += trun.getSampleCount();
	                    List<TrackRunBox.Entry> recs = trun.getEntries();
	                    for (TrackRunBox.Entry rec : recs) {
	                        if (trun.isSampleCompositionTimeOffsetPresent()) {
	                            CompositionTimeToSample.Entry ctts = new CompositionTimeToSample.Entry((int) trun.getSampleCount(), (int) rec.getSampleCompositionTimeOffset());
	                            compositionTimes.add(ctts);
	                        }
	                        sampleSizes.add(rec.getSampleSize());
	                        if (trun.isSampleDurationPresent()) {
	                            videoSampleDuration += rec.getSampleDuration();
	                        }
	                    }
	                }
	                // SampleToChunkBox.Entry
	                videoSamples = new long[sampleSizes.size()];
	                for (int i = 0; i < videoSamples.length; i++) {
	                    videoSamples[i] = sampleSizes.remove();
	                }
	                videoChunkOffsets = new long[dataOffsets.size()];
	                for (int i = 0; i < videoChunkOffsets.length; i++) {
	                    videoChunkOffsets[i] = dataOffsets.remove();
	                }
	            }
	        }
	    } catch (Exception e) {
	        log.error("Exception decoding header / atoms", e);
	    }
		
	}
	
	/**
	 * Process the video information contained in the atoms.
	 * 
	 * @param stbl
	 * @param vse VisualSampleEntry
	 * @param scale timescale
	 */
	private void processVideoBox(SampleTableBox stbl, VisualSampleEntry vse, long scale) {
		// get codec
		String codecName = vse.getType();
		//set the video codec here - may be avc1 or mp4v
		setVideoCodecId(codecName);
		if ("avc1".equals(codecName)) {
			//AvcConfigurationBox avc1 = vse.getBoxes(AvcConfigurationBox.class).get(0);
			AvcConfigurationBox avc1 = vse.getBoxes(AvcConfigurationBox.class).get(0);
			avcLevel = avc1.getAvcLevelIndication();
			//log.debug("AVC level: {}", avcLevel);
			avcProfile = avc1.getAvcProfileIndication();
			//log.debug("AVC Profile: {}", avcProfile);
			AvcDecoderConfigurationRecord avcC = avc1.getavcDecoderConfigurationRecord();
			if (avcC != null) {
				long videoConfigContentSize = avcC.getContentSize();
				//log.debug("AVCC size: {}", videoConfigContentSize);
				IoBuffer byteBuffer = IoBuffer.allocate((int) videoConfigContentSize);
				avc1.avcDecoderConfigurationRecord.getContent(byteBuffer.buf());
				byteBuffer.flip();
				videoDecoderBytes = new byte[byteBuffer.limit()];
				byteBuffer.get(videoDecoderBytes);
			} else {
				// quicktime and ipods use a pixel aspect atom (pasp)
				// since we have no avcC check for this and avcC may be a child
				log.warn("avcC atom not found; we may need to modify this to support pasp atom");
			}
		} else if ("mp4v".equals(codecName)) {
			if (vse.getBoxes(ESDescriptorBox.class).size() > 0) {
				// look for esds
				ESDescriptorBox esds = vse.getBoxes(ESDescriptorBox.class).get(0);
				if (esds != null) {
					ESDescriptor descriptor = esds.getEsDescriptor();
					//log.debug("ES descriptor: {}", descriptor);
					if (descriptor != null) {
						DecoderConfigDescriptor decConf = descriptor.getDecoderConfigDescriptor();
						if (decConf != null) {
							DecoderSpecificInfo decInfo = decConf.getDecoderSpecificInfo();
							ByteBuffer byteBuffer = decInfo.serialize();		
							videoDecoderBytes = new byte[byteBuffer.limit()];
							byteBuffer.get(videoDecoderBytes);
						}
					}
				}
			}
		} else {
			log.debug("Unrecognized video codec: {} compressor name: {}", codecName, vse.getCompressorname());
		}
		
		processVideoStbl(stbl, scale);
	}
	
	/**
	 * Process the video information contained in the atoms.
	 * 
	 * @param stbl
	 * @param scale timescale
	 */
	private void processVideoBox(SampleTableBox stbl, long scale) {
		/*
		AvcConfigurationBox avcC = (AvcConfigurationBox) Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/drmi/avcC");
		if (avcC != null) {
			long videoConfigContentSize = avcC.getContentSize();
			log.debug("AVCC size: {}", videoConfigContentSize);
//			ByteBuffer byteBuffer = ByteBuffer.allocate((int) videoConfigContentSize);
//			avc1.avcDecoderConfigurationRecord.getContent(byteBuffer);
//			byteBuffer.flip();
//			videoDecoderBytes = new byte[byteBuffer.limit()];
//			byteBuffer.get(videoDecoderBytes);
		} else {
			log.warn("avcC atom not found");
		}
		*/
		processVideoStbl(stbl, scale);
	}
	
	/**
	 * Process an stbl atom with containing video information.
	 * 
	 * @param stbl
	 * @param scale
	 */
	private void processVideoStbl(SampleTableBox stbl, long scale) {
		
		// stsc - has Records
		SampleToChunkBox stsc = stbl.getSampleToChunkBox(); // stsc
		if (stsc != null) {
			//log.debug("Sample to chunk atom found");
			videoSamplesToChunks = stsc.getEntries();
//			log.info("Video samples to chunks: {}", videoSamplesToChunks.size());
//			for(Entry e : videoSamplesToChunks) {
//				log.info("Video Entry {}", e.toString());
//			}
		}
		// stsz - has Samples
		SampleSizeBox stsz = stbl.getSampleSizeBox(); // stsz
		if (stsz != null) {
			//log.debug("Sample size atom found");
			videoSamples = stsz.getSampleSizes();
			// if sample size is 0 then the table must be checked due to variable sample sizes
			//log.debug("Sample size: {}", stsz.getSampleSize());
			videoSampleCount = stsz.getSampleCount();
			//log.debug("Sample count: {}", videoSampleCount);
		}
		// stco - has Chunks
		ChunkOffsetBox stco = stbl.getChunkOffsetBox(); // stco / co64
		if (stco != null) {
			//log.debug("Chunk offset atom found");
			videoChunkOffsets = stco.getChunkOffsets();
			//log.debug("Chunk count: {}", videoChunkOffsets.length);
		} else {
			// co64 - has Chunks
			List<ChunkOffset64BitBox> stblBoxes = stbl.getBoxes(ChunkOffset64BitBox.class);
            if (stblBoxes != null && !stblBoxes.isEmpty()) {
                ChunkOffset64BitBox co64 = stblBoxes.get(0);
                if (co64 != null) {
                    videoChunkOffsets = co64.getChunkOffsets();
                    // double the timescale for video, since it seems to run at
                    // half-speed when co64 is used (seems hacky)
                    //videoTimeScale = scale * 2.0;
                    //log.debug("Video time scale: {}", videoTimeScale);
                }
            }
		}
		// stss - has Sync - no sync means all samples are keyframes
		SyncSampleBox stss = stbl.getSyncSampleBox(); // stss
		if (stss != null) {
			//log.debug("Sync sample atom found");
			syncSamples = stss.getSampleNumber();
			//log.debug("Keyframes: {}", syncSamples.length);
		}
		// stts - has TimeSampleRecords
		TimeToSampleBox stts = stbl.getTimeToSampleBox(); // stts
		if (stts != null) {
			//log.debug("Time to sample atom found");
			List<TimeToSampleBox.Entry> records = stts.getEntries();
			//log.debug("Video time to samples: {}", records.size());
			// handle instance where there are no actual records (bad f4v?)
			if (records.size() > 0) {
				TimeToSampleBox.Entry rec = records.get(0);
				//log.debug("Samples = {} delta = {}", rec.getCount(), rec.getDelta());
				//if we have 1 record it means all samples have the same duration
				videoSampleDuration = rec.getDelta();
			}
		}
		// ctts - (composition) time to sample
		CompositionTimeToSample ctts = stbl.getCompositionTimeToSample(); // ctts
		if (ctts != null) {
			//log.debug("Composition time to sample atom found");
			compositionTimes = ctts.getEntries();
			//log.debug("Record count: {}", compositionTimes.size());
			/**
			if (log.isTraceEnabled()) {
				for (CompositionTimeToSample.Entry rec : compositionTimes) {
					double offset = rec.getOffset();
					if (scale > 0d) {
						offset = (offset / (double) scale) * 1000.0;
						rec.setOffset((int) offset);
					}
					log.trace("Samples = {} offset = {}", rec.getCount(), rec.getOffset());
				}
			}
			*/
		}
		// sdtp - sample dependency type
		/*
		SampleDependencyTypeBox sdtp = stbl.getSampleDependencyTypeBox(); // sdtp
		if (sdtp != null) {
			//log.debug("Independent and disposable samples atom found");
			List<SampleDependencyTypeBox.Entry> recs = sdtp.getEntries();
			for (SampleDependencyTypeBox.Entry rec : recs) {
				log.debug("{}", rec);
			}
		}
		*/

	}
	
	/**
	 * Process the audio information contained in the atoms.
	 * 
	 * @param stbl
	 * @param ase AudioSampleEntry
	 * @param scale timescale
	 */
	private void processAudioBox(SampleTableBox stbl, AudioSampleEntry ase, long scale) {
		// get codec
		String codecName = ase.getType();
		// set the audio codec here - may be mp4a or...
		setAudioCodecId(codecName);
		//log.debug("Sample size: {}", ase.getSampleSize());
		long ats = ase.getSampleRate();
		// skip invalid audio time scale
		if (ats > 0) {
			audioTimeScale = ats * 1.0;
		}
		//log.debug("Sample rate (audio time scale): {}", audioTimeScale);
		audioChannels = ase.getChannelCount();
		//log.debug("Channels: {}", audioChannels);
		if (ase.getBoxes(ESDescriptorBox.class).size() > 0) {
			// look for esds
			ESDescriptorBox esds = ase.getBoxes(ESDescriptorBox.class).get(0);
			if (esds == null) {
				log.debug("esds not found in default path");
				// check for decompression param atom
				AppleWaveBox wave = ase.getBoxes(AppleWaveBox.class).get(0);
				if (wave != null) {
					log.debug("wave atom found");
					// wave/esds
					esds = wave.getBoxes(ESDescriptorBox.class).get(0);
					if (esds == null) {
						//log.debug("esds not found in wave");
						// mp4a/esds
						//AC3SpecificBox mp4a = wave.getBoxes(AC3SpecificBox.class).get(0);
						//esds = mp4a.getBoxes(ESDescriptorBox.class).get(0);
					}
				}
			}
			//mp4a: esds
			if (esds != null) {
				// http://stackoverflow.com/questions/3987850/mp4-atom-how-to-discriminate-the-audio-codec-is-it-aac-or-mp3
				ESDescriptor descriptor = esds.getEsDescriptor();
				if (descriptor != null) {
					DecoderConfigDescriptor configDescriptor = descriptor.getDecoderConfigDescriptor();
					AudioSpecificConfig audioInfo = configDescriptor.getAudioSpecificInfo();
					if (audioInfo != null) {
						audioDecoderBytes = audioInfo.getConfigBytes();
						/* the first 5 (0-4) bits tell us about the coder used for aacaot/aottype
						 * http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio
						 0 - NULL
						 1 - AAC Main (a deprecated AAC profile from MPEG-2)
						 2 - AAC LC or backwards compatible HE-AAC 
						 3 - AAC Scalable Sample Rate
						 4 - AAC LTP (a replacement for AAC Main, rarely used)
						 5 - HE-AAC explicitly signaled (Non-backward compatible)
						23 - Low Delay AAC
						29 - HE-AACv2 explicitly signaled
						32 - MP3on4 Layer 1
						33 - MP3on4 Layer 2
						34 - MP3on4 Layer 3
						*/
						byte audioCoderType = audioDecoderBytes[0];
						//match first byte
						switch (audioCoderType) {
							case 0x02:
								//log.debug("Audio type AAC LC");
							case 0x11: //ER (Error Resilient) AAC LC
								//log.debug("Audio type ER AAC LC");
							default:
								audioCodecType = 1; //AAC LC
								break;
							case 0x01:
								//log.debug("Audio type AAC Main");
								audioCodecType = 0; //AAC Main
								break;
							case 0x03:
								//log.debug("Audio type AAC SBR");
								audioCodecType = 2; //AAC LC SBR
								break;
							case 0x05:
							case 0x1d:
								//log.debug("Audio type AAC HE");
								audioCodecType = 3; //AAC HE
								break;
							case 0x20:
							case 0x21:
							case 0x22:
								//log.debug("Audio type MP3");
								audioCodecType = 33; //MP3
								audioCodecId = "mp3";
								break;
						}
						//log.debug("Audio coder type: {} {} id: {}", new Object[] { audioCoderType, Integer.toBinaryString(audioCoderType), audioCodecId });
					} else {
						//log.debug("Audio specific config was not found");
						DecoderSpecificInfo info = configDescriptor.getDecoderSpecificInfo();
						if (info != null) {
							//log.debug("Decoder info found: {}", info.getTag());
							// qcelp == 5
						}
					}
				} else {
					log.warn("No ES descriptor found");
				}
			}
		} else {
			log.warn("Audio sample entry had no descriptor");
		}
		
		processAudioStbl(stbl, scale);
	}
	
	/**
	 * Process the audio information contained in the atoms.
	 * 
	 * @param stbl
	 * @param scale timescale
	 */
	private void processAudioBox(SampleTableBox stbl, long scale) {

		processAudioStbl(stbl, scale);
	}
	
	private void processAudioStbl(SampleTableBox stbl, long scale) {
		//stsc - has Records
		SampleToChunkBox stsc = stbl.getSampleToChunkBox(); // stsc
		if (stsc != null) {
			//log.debug("Sample to chunk atom found");
			audioSamplesToChunks = stsc.getEntries();
			log.debug("### Audio samples to chunks: {}", audioSamplesToChunks.size());
			// handle instance where there are no actual records (bad f4v?)
		}
		//stsz - has Samples
		SampleSizeBox stsz = stbl.getSampleSizeBox(); // stsz
		if (stsz != null) {
			//log.debug("Sample size atom found");
			audioSamples = stsz.getSampleSizes();
			//log.debug("Samples: {}", audioSamples.length);
			// if sample size is 0 then the table must be checked due to variable sample sizes
			audioSampleSize = stsz.getSampleSize();
			//log.debug("Sample size: {}", audioSampleSize);
			//long audioSampleCount = stsz.getSampleCount();
			//log.debug("Sample count: {}", audioSampleCount);
		}
		//stco - has Chunks
		ChunkOffsetBox stco = stbl.getChunkOffsetBox(); // stco / co64
		if (stco != null) {
			//log.debug("Chunk offset atom found");
			audioChunkOffsets = stco.getChunkOffsets();
			//log.debug("Chunk count: {}", audioChunkOffsets.length);
		} else {
			//co64 - has Chunks
			ChunkOffset64BitBox co64 = stbl.getBoxes(ChunkOffset64BitBox.class).get(0);
			if (co64 != null) {
				//log.debug("Chunk offset (64) atom found");
				audioChunkOffsets = co64.getChunkOffsets();
				//log.debug("Chunk count: {}", audioChunkOffsets.length);
			}
		}
		//stts - has TimeSampleRecords
		TimeToSampleBox stts = stbl.getTimeToSampleBox(); // stts
		if (stts != null) {
			//log.debug("Time to sample atom found");
			List<TimeToSampleBox.Entry> records = stts.getEntries();
			//log.debug("Audio time to samples: {}", records.size());
			// handle instance where there are no actual records (bad f4v?)
			if (records.size() > 0) {
				TimeToSampleBox.Entry rec = records.get(0);
				//log.debug("Samples = {} delta = {}", rec.getCount(), rec.getDelta());
				//if we have 1 record it means all samples have the same duration
				audioSampleDuration = rec.getDelta();
			}
		}
		// sdtp - sample dependency type
		//SampleDependencyTypeBox sdtp = stbl.getSampleDependencyTypeBox(); // sdtp
		/**
		if (sdtp != null) {
			//log.debug("Independent and disposable samples atom found");
			List<SampleDependencyTypeBox.Entry> recs = sdtp.getEntries();
			for (SampleDependencyTypeBox.Entry rec : recs) {
				log.debug("{}", rec);
			}
		}
		*/	
	}

	/**
	 * Get the total readable bytes in a file or IoBuffer.
	 *
	 * @return          Total readable bytes
	 */
	public long getTotalBytes() {
		try {
			return channel.size();
		} catch (Exception e) {
			log.error("Error getTotalBytes", e);
		}
		if (file != null) {
			//just return the file size
			return file.length();
		} else {
			return 0;
		}
	}

	/**
	 * Get the current position in a file or IoBuffer.
	 *
	 * @return           Current position in a file
	 */
	private long getCurrentPosition() {
		try {
			//if we are at the end of the file drop back to mdat offset
			/*
			if (channel.position() == channel.size()) {
				//log.debug("Reached end of file, going back to data offset");
				channel.position(mdatOffset);
			}
			return channel.position();
			*/
			return channel.position();
			
		} catch (Exception e) {
			log.error("Error getCurrentPosition", e);
			return 0;
		}
	}

	/** {@inheritDoc} */
	public boolean hasVideo() {
		return hasVideo;
	}

	/**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
	public IoBuffer getFileData() {
		// TODO as of now, return null will disable cache
		// we need to redesign the cache architecture so that
		// the cache is layered underneath FLVReader not above it,
		// thus both tag cache and file cache are feasible.
		return null;
	}

	/** {@inheritDoc}
	 */
	public IStreamableFile getFile() {
		// TODO wondering if we need to have a reference
		return null;
	}

	/** {@inheritDoc}
	 */
	public int getOffset() {
		// XXX what's the difference from getBytesRead
		return 0;
	}

	/** {@inheritDoc}
	 */
	public long getBytesRead() {
		// XXX should summarize the total bytes read or
		// just the current position?
		return getCurrentPosition();
	}

	/** {@inheritDoc} */
	public long getDuration() {
		return duration / timeScale;
	}

	public String getVideoCodecId() {
		return videoCodecId;
	}

	public String getAudioCodecId() {
		return audioCodecId;
	}

	/** {@inheritDoc}
	 */
	public boolean hasMoreTags() {
		return currentFrame < frames.size();
	}

	/**
	 * Create tag for metadata event.
	 *
	 * Information mostly from http://www.kaourantin.net/2007/08/what-just-happened-to-video-on-web_20.html
	 * <pre>
		width: Display width in pixels.
		height: Display height in pixels.
		duration: Duration in seconds. But unlike for FLV files this field will always be present.
		videocodecid: Usually a string such as "avc1" or "VP6F", for H.264 we report 'avc1'.
		audiocodecid: Usually a string such as ".mp3" or "mp4a", for AAC we report 'mp4a' and MP3 we report '.mp3'.
	    avcprofile: AVC profile number, values of 66, 77, 88, 100, 110, 122 or 144; which correspond to the H.264 profiles.
	    avclevel: AVC IDC level number, values between 10 and 51.
	    aottype: Either 0, 1 or 2. This corresponds to AAC Main, AAC LC and SBR audio types.
	    moovposition: The offset in bytes of the moov atom in a file.
	    trackinfo: An array of objects containing various infomation about all the tracks in a file
	      ex.
	    	trackinfo[0].length: 7081
	    	trackinfo[0].timescale: 600
	    	trackinfo[0].sampledescription.sampletype: avc1
	    	trackinfo[0].language: und
	    	trackinfo[1].length: 525312
	    	trackinfo[1].timescale: 44100
	    	trackinfo[1].sampledescription.sampletype: mp4a
	    	trackinfo[1].language: und
	    
	    chapters: As mentioned above information about chapters in audiobooks.
		seekpoints: Array that lists the available keyframes in a file as time stamps in milliseconds. 
				This is optional as the MP4 file might not contain this information. Generally speaking, 
				most MP4 files will include this by default. You can directly feed the values into NetStream.seek();
	    videoframerate: The frame rate of the video if a monotone frame rate is used. 
	    		Most videos will have a monotone frame rate.
	    audiosamplerate: The original sampling rate of the audio track.
	    audiochannels: The original number of channels of the audio track.
		progressivedownloadinfo: Object that provides information from the "pdin" atom. This is optional 
				and many files will not have this field.
		tags: Array of key value pairs representing the information present in the "ilst" atom, which is 
				the equivalent of ID3 tags for MP4 files. These tags are mostly used by iTunes. 
	 * </pre>
	 *
	 * @return         Metadata event tag
	 */
	ITag createFileMeta() {
		//log.debug("Creating onMetaData");
		// Create tag for onMetaData event
		IoBuffer buf = IoBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> props = new HashMap<Object, Object>();
		// Duration property
		props.put("duration", ((double) duration / (double) timeScale));
		props.put("width", width);
		props.put("height", height);
		// Video codec id
		props.put("videocodecid", videoCodecId);
		props.put("avcprofile", avcProfile);
		props.put("avclevel", avcLevel);
		props.put("videoframerate", fps);
		// Audio codec id - watch for mp3 instead of aac
		props.put("audiocodecid", audioCodecId);
		props.put("aacaot", audioCodecType);
		props.put("audiosamplerate", audioTimeScale);
		props.put("audiochannels", audioChannels);
		// position of the moov atom
		//props.put("moovposition", moovOffset);
		//props.put("chapters", ""); //this is for f4b - books
		if (seekPoints != null) {
			//log.debug("Seekpoint list size: {}", seekPoints.size());
			props.put("seekpoints", seekPoints);
		}
		//tags will only appear if there is an "ilst" atom in the file
		//props.put("tags", "");
		List<Map<String, Object>> arr = new ArrayList<Map<String, Object>>(2);
		if (hasAudio) {
			Map<String, Object> audioMap = new HashMap<String, Object>(4);
			audioMap.put("timescale", audioTimeScale);
			audioMap.put("language", "und");

			List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
			audioMap.put("sampledescription", desc);

			Map<String, String> sampleMap = new HashMap<String, String>(1);
			sampleMap.put("sampletype", audioCodecId);
			desc.add(sampleMap);

			if (audioSamples != null) {
				if (audioSampleDuration > 0) {
					audioMap.put("length_property", audioSampleDuration * audioSamples.length);
				 }
				//release some memory, since we're done with the vectors
				audioSamples = null;
			}
			arr.add(audioMap);
		}
		if (hasVideo) {
			Map<String, Object> videoMap = new HashMap<String, Object>(3);
			videoMap.put("timescale", videoTimeScale);
			videoMap.put("language", "und");

			List<Map<String, String>> desc = new ArrayList<Map<String, String>>(1);
			videoMap.put("sampledescription", desc);

			Map<String, String> sampleMap = new HashMap<String, String>(1);
			sampleMap.put("sampletype", videoCodecId);
			desc.add(sampleMap);
			if (videoSamples != null) {
				if (videoSampleDuration > 0) {
					videoMap.put("length_property", videoSampleDuration * videoSamples.length);
				}
				//release some memory, since we're done with the vectors
				videoSamples = null;
			}
			arr.add(videoMap);
		}
		props.put("trackinfo", arr);
		//set this based on existence of seekpoints
		props.put("canSeekToEnd", (seekPoints != null));
		out.writeMap(props);
		buf.flip();
		//now that all the meta properties are done, update the duration
		duration = Math.round(duration * 1000d);
		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
		result.setBody(buf);
		out.clearReferences();
		return result;
	}

	/**
	 * Tag sequence
	 * MetaData, Video config, Audio config, remaining audio and video 
	 * 
	 * Packet prefixes:
	 * 17 00 00 00 00 = Video extra data (first video packet)
	 * 17 01 00 00 00 = Video keyframe
	 * 27 01 00 00 00 = Video interframe
	 * af 00 ...   06 = Audio extra data (first audio packet)
	 * af 01          = Audio frame
	 * 
	 * Audio extra data(s): 
	 * af 00                = Prefix
	 * 11 90 4f 14          = AAC Main   = aottype 0 // 11 90
	 * 12 10                = AAC LC     = aottype 1
	 * 13 90 56 e5 a5 48 00 = HE-AAC SBR = aottype 2
	 * 06                   = Suffix
	 * 
	 * Still not absolutely certain about this order or the bytes - need to verify later
	 */
	private void createPreStreamingTags(int timestamp, boolean clear) {
		//log.debug("Creating pre-streaming tags");
		if (clear) {
			firstTags.clear();
		}
		ITag tag = null;
		IoBuffer body = null;
		if (hasVideo) {
			//video tag #1
			body = IoBuffer.allocate(41);
			body.setAutoExpand(true);
			body.put(PREFIX_VIDEO_CONFIG_FRAME); //prefix
			if (videoDecoderBytes != null) {
				//because of other processing we do this check
				body.put(videoDecoderBytes);
			}
			tag = new Tag(IoConstants.TYPE_VIDEO, timestamp, body.position(), null, 0);
			body.flip();
			tag.setBody(body);
			//add tag
			firstTags.add(tag);
		}
		// TODO: Handle other mp4 container audio codecs like mp3
		// mp3 header magic number ((int & 0xffe00000) == 0xffe00000) 
		if (hasAudio) {
			//audio tag #1
			if (audioDecoderBytes != null) {
				//because of other processing we do this check
				
				body = IoBuffer.allocate(audioDecoderBytes.length + 3);
				body.setAutoExpand(true);
				body.put(PREFIX_AUDIO_CONFIG_FRAME); //prefix
				/** Audio packet prefix for the decoder frame */
				body.put(audioDecoderBytes);
				body.put((byte) 0x06); //suffix
				tag = new Tag(IoConstants.TYPE_AUDIO, timestamp, body.position(), null, 0);
				body.flip();
				tag.setBody(body);
				//add tag
				firstTags.add(tag);
			} else if(audioCodecType == 0 || audioCodecType == 1 || audioCodecType == 2) { // aac
				log.info("Audio aac decoder bytes were not available, set default value");
				//default to aac-lc when the esds doesnt contain descripter bytes
				tag = createPreAudioStreamingTags();
				firstTags.add(tag);
			} else { // mp3 ?
				//log.info("Audio decoder bytes were not available");
			}
		}
	}
	
	private ITag createPreAudioStreamingTags() {
    	log.debug("Creating pre-streaming tags");
    	IoBuffer body = IoBuffer.allocate(7);
		body.setAutoExpand(true);
		body.put(new byte[]{(byte) 0xaf, (byte) 0}); //prefix
		
		switch (audioCodecType) {
		case 0:
			body.put(MP4Reader.AUDIO_CONFIG_FRAME_AAC_MAIN);
			break;
		case 1:
			body.put(MP4Reader.AUDIO_CONFIG_FRAME_AAC_LC);
			break;
		case 2:
			body.put(MP4Reader.AUDIO_CONFIG_FRAME_SBR);		
			break;
		default:
			break;
		}
		body.put((byte) 0x06); //suffix
		ITag tag = new Tag(IoConstants.TYPE_AUDIO, 0, body.position(), null, 0);
		body.flip();
		tag.setBody(body);
		
		return tag;
    }

	/**
	 * Packages media data for return to providers
	 */
	public ITag readTag() {
		ITag tag = null;
		//empty-out the pre-streaming tags first
		if (!firstTags.isEmpty()) {
			//log.debug("Returning pre-tag");
			// Return first tags before media data
			return firstTags.removeFirst();
		}
		//log.debug("Read tag - sample {} prevFrameSize {} audio: {} video: {}", new Object[]{currentSample, prevFrameSize, audioCount, videoCount});
		//get the current frame
		MP4Frame frame = frames.get(currentFrame);
		//log.debug("Playback #{} {}", currentFrame, frame);
		int sampleSize = frame.getSize();
		int time = (int) Math.round(frame.getTime() * 1000.0);
		//log.debug("Read tag - dst: {} base: {} time: {}", new Object[]{frameTs, baseTs, time});
		long samplePos = frame.getOffset();
		//log.debug("Read tag - samplePos {}", samplePos);
		//determine frame type and packet body padding
		byte type = frame.getType();
		//assume video type
		int pad = 5;
		if (type == TYPE_AUDIO) {
			pad = 2;
		}
		//create a byte buffer of the size of the sample
		ByteBuffer data = ByteBuffer.allocate(sampleSize + pad);
		
		try {
			//prefix is different for keyframes
			if (type == TYPE_VIDEO) {
				if (frame.isKeyFrame()) {
					//log.debug("Writing keyframe prefix");
					data.put(PREFIX_VIDEO_KEYFRAME);
				} else {
					//log.debug("Writing interframe prefix");
					data.put(PREFIX_VIDEO_FRAME);
				}
				// match the sample with its ctts / mdhd adjustment time
				
				//int timeOffset = prevVideoTS != -1 ? time - prevVideoTS : 0;
				int timeOffset = (int)(frame.getTimeOffset() / videoTimeScale * 1000);;
				data.put((byte) ((timeOffset >>> 16) & 0xff));
				data.put((byte) ((timeOffset >>> 8) & 0xff));
				data.put((byte) (timeOffset & 0xff));
	
				// track video frame count
				videoCount++;
				prevVideoTS = time;
			} else {
				//log.debug("Writing audio prefix");
				data.put(PREFIX_AUDIO_FRAME);
				// track audio frame count
				audioCount++;
			}
			//do we need to add the mdat offset to the sample position?
			channel.position(samplePos);
			channel.read(data);
		} catch (IOException e) {
			log.error("Error on channel position / read", e);
		}
		//data.flip();
		//byte[] tmp = new byte[data.remaining()];
		//data.get(tmp, 0, tmp.length);
		IoBuffer payload = IoBuffer.wrap(data.array());
		//create the tag
		tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
		//log.debug("Read tag - type: {} body size: {}", (type == TYPE_AUDIO ? "Audio" : "Video"), tag.getBodySize());
		//increment the frame number
		currentFrame++;
		//set the frame / tag size
		prevFrameSize = tag.getBodySize();
		//log.debug("Tag: {}", tag);		
		return tag;
	}

	/**
	 * Performs frame analysis and generates metadata for use in seeking. All the frames
	 * are analyzed and sorted together based on time and offset.
	 */
	public List<MP4Frame> analyzeFrames() {
		
		//StopWatch sw = new StopWatch();
		//sw.start();
		
		//log.debug("Analyzing frames");
		// Maps positions, samples, timestamps to one another
		timePosMap = new HashMap<Integer, Long>();
		samplePosMap = new HashMap<Integer, Long>();
		// tag == sample
		int sample = 1;
		// position
		Long pos = null;
		// if audio-only, skip this
		
		//videoSamplesToChunks: firstChunk & Sample Per Chunk info
		if (videoSamplesToChunks != null) {
			//log.info("videoSamplesToChunks");
			// handle composite times
			int compositeIndex = 0;
			CompositionTimeToSample.Entry compositeTimeEntry = null;
			if (compositionTimes != null && !compositionTimes.isEmpty()) {
				compositeTimeEntry = compositionTimes.remove(0);
			}
			for (int i = 0; i < videoSamplesToChunks.size(); i++) {
				Entry record = videoSamplesToChunks.get(i);
				long firstChunk = record.getFirstChunk();
				long lastChunk = videoChunkOffsets.length; //Sample arrays
				if (i < videoSamplesToChunks.size() - 1) {
					Entry nextRecord = videoSamplesToChunks.get(i + 1);
					lastChunk = nextRecord.getFirstChunk() - 1;
				}
				for (long chunk = firstChunk; chunk <= lastChunk; chunk++) {
					long sampleCount = record.getSamplesPerChunk();
					pos = videoChunkOffsets[(int) (chunk - 1)];
					while (sampleCount > 0) {
						//log.debug("Position: {}", pos);
						samplePosMap.put(sample, pos);
						//calculate ts
						double ts = (videoSampleDuration * (sample - 1)) / videoTimeScale;
						//check to see if the sample is a keyframe
						boolean keyframe = false;
						//some files appear not to have sync samples
						if (syncSamples != null) {
							keyframe = ArrayUtils.contains(syncSamples, sample);
							if (seekPoints == null) {
								seekPoints = new LinkedList<Integer>();
							}
							// get the timestamp
							int frameTs = (int) Math.round(ts * 1000.0);
							// add each key frames timestamp to the seek points list
							if (keyframe) {
								seekPoints.add(frameTs);
							}
							timePosMap.put(frameTs, pos);
						} else {
							log.warn("No sync samples available");
						}
						//size of the sample
						int size = (int) videoSamples[sample - 1];
						// exclude data that is not within the mdat box
						//						if ((moovOffset < mdatOffset && pos > mdatOffset) || (moovOffset > mdatOffset && pos < moovOffset)) {
						//create a frame
						MP4Frame frame = new MP4Frame();
						frame.setKeyFrame(keyframe);
						frame.setOffset(pos);
						frame.setSize(size);
						frame.setTime(ts);
						frame.setType(TYPE_VIDEO);
						//set time offset value from composition records
						if (compositeTimeEntry != null) {
							// how many samples have this offset
							int consecutiveSamples = compositeTimeEntry.getCount();
							frame.setTimeOffset(compositeTimeEntry.getOffset());
							// increment our count
							compositeIndex++;
							if (compositeIndex - consecutiveSamples == 0) {
								// ensure there are still times available
								if (!compositionTimes.isEmpty()) {
									// get the next one
									compositeTimeEntry = compositionTimes.remove(0);
								}
								// reset
								compositeIndex = 0;
							}
							//log.debug("Composite sample #{} {}", sample, frame);
						}
						// add the frame
						frames.add(frame);
						pos += size;
						sampleCount--;
						sample++;
					}
				}
			}
			//log.debug("Sample position map (video): {}", samplePosMap);
		}
		// if video-only, skip this
		if (audioSamplesToChunks != null) {
			//log.info("audioSamplesToChunks");
			//add the audio frames / samples / chunks		
			sample = 1;
			for (int i = 0; i < audioSamplesToChunks.size(); i++) {
				Entry record = audioSamplesToChunks.get(i);
				long firstChunk = record.getFirstChunk();
				long lastChunk = audioChunkOffsets.length;
				if (i < audioSamplesToChunks.size() - 1) {
					Entry nextRecord = audioSamplesToChunks.get(i + 1);
					lastChunk = nextRecord.getFirstChunk() - 1;
				}
				for (long chunk = firstChunk; chunk <= lastChunk; chunk++) {
					long sampleCount = record.getSamplesPerChunk();
					pos = audioChunkOffsets[(int) (chunk - 1)];
					while (sampleCount > 0) {
						//calculate ts
						double ts = (audioSampleDuration * (sample - 1)) / audioTimeScale;
						
						// sample size
						int size = 0;
						// if we have no samples, skip size check as its probably not aac
						if (audioSamples.length > 0) {
							//update sample size 
							size = (int) audioSamples[sample - 1];
							// skip empty AAC data which is 6 bytes long
							/*
							if (size == 6) {
								try {
									// get current pos
									long position = channel.position();
									// jump to data position
									channel.position(pos);
									// create buffer to store bytes so we can check them
									IoBuffer dst = IoBuffer.allocate(6);
									// read the data
									channel.read(dst.buf());
									// flip it
									dst.flip();
									// reset the position
									channel.position(position);
									byte[] tmp = new byte[dst.remaining()];
									dst.get(tmp, 0, tmp.length);
									//log.trace("Audio bytes: {} equal: {}", HexDump.byteArrayToHexString(tmp), Arrays.equals(EMPTY_AAC, tmp));
									if (Arrays.equals(EMPTY_AAC, tmp)) {
										//log.trace("Skipping empty AAC data frame");
										// update counts
										pos += size;
										sampleCount--;
										sample++;
										// read next
										continue;
									}
								} catch (IOException e) {
									log.warn("Exception during audio analysis", e);
								}
							}
							*/
						}
						// set audio sample size
						size = (int) (size != 0 ? size : audioSampleSize);
						// exclude data that is not within the mdat box
						//if (pos >= mdatOffset) {
							//create a frame
						MP4Frame frame = new MP4Frame();
						frame.setOffset(pos);
						frame.setSize(size);
						frame.setTime(ts);
						frame.setType(TYPE_AUDIO);
						//log.info("Audio MP4Frame === " + frame.toString());
						frames.add(frame);
							//log.debug("Sample #{} {}", sample, frame);
//						} else {
//							log.warn("Skipping audio frame with invalid position");
//						}
						// update counts
						pos += size;
						sampleCount--;
						sample++;
					}
				}
			}
		}
		//sort the frames
		log.info("Frames size : {}, Total Duration : {}",frames.size(), duration);
		//log.error("Frames: {}", frames);
		
		Collections.sort(frames);
		
//		for(MP4Frame f : frames) {
//			log.info("\n{}", f.toString());
//		}
		
		//sw.stop();
		//log.info("Frame sort duration :: {}ms", sw.getTotalTimeMillis());
		
		//log.debug("Frames count: {}", frames.size());
		//release some memory, since we're done with the vectors
		if (audioSamplesToChunks != null) {
			audioChunkOffsets = null;
			audioSamplesToChunks.clear();
			audioSamplesToChunks = null;
		}
		if (videoSamplesToChunks != null) {
			videoChunkOffsets = null;
			videoSamplesToChunks.clear();
			videoSamplesToChunks = null;
		}
		if (syncSamples != null) {
			syncSamples = null;
		}
		
		return frames;
	}

	/**
	 * Put the current position to pos. The caller must ensure the pos is a valid one.
	 *
	 * @param pos position to move to in file / channel
	 */
	public void position(long pos) {
		
		//StopWatch sw = new StopWatch();
		int loop_cnt = 0;
		int search_num = 0;
		//log.info("Position: {}", pos);
		//log.debug("Current frame: {}", currentFrame);
		try {
			
			//sw.start();
			int len = frames.size();
			MP4Frame frame = null;
			
			int start_num = binarySearch(pos, frames);
			int idx = start_num;
			
			while (true) {
				frame = frames.get(idx);
				long offset = frame.getOffset();
				
				if (pos == offset || (offset > pos && frame.isKeyFrame())) {
					if (!frame.isKeyFrame()) {
						//log.debug("Frame #{} was not a key frame, so trying again..", f);
						continue;
					}
					createPreStreamingTags((int) (frame.getTime() * 1000), true);
					currentFrame = idx;
					break;
				}
				if(loop_cnt % 2 == 0) {
					idx = start_num + search_num >= len ? len : start_num + search_num;
				} else if(loop_cnt % 2 == 1) {
					idx = start_num - search_num < 0 ? 0 : start_num - search_num;
					search_num ++;
				}
				loop_cnt ++;
				
				if(idx < 0 && idx >= len) {
					throw new Exception("### THERE IS NO FRAME In Position ####");
				}
			}
		} catch(Exception e) {
			log.error("# Fail to find Position \n", e.getMessage());
			e.printStackTrace();
		} finally {
			//sw.stop();
			//log.info("### Find Position :: Duration [{}ms], Loop Count [{}]", sw.getTotalTimeMillis(), loop_cnt);
		}
	}
	
	private int binarySearch(long value, List<MP4Frame> input) {
		int mid;
		int left = 0;
		int right = input.size() - 1;
		int rtnVal = 0;
		
		while (right >= left) {
			mid = (right + left) / 2;
			
			if(mid <= 1) {
				rtnVal = 0;
				break;
			} else if(mid == input.size() - 1) {
				rtnVal = mid;
				break;
			} else {
				if(( input.get(mid-1).getOffset() < value && value <= input.get(mid).getOffset() )) {
					//log.error("binarySearch Value {},  Offset {}, start_num {} ", input.get(mid-1).getOffset(), value, mid-1);
					rtnVal = mid-1;
					break;
				} 
			}
			
			if(value < input.get(mid).getOffset()) {
				right = mid - 1;
			} else {
				left = mid + 1;
			}
		}
		
		return rtnVal;
	}

	/** {@inheritDoc}
	 */
	public void close() {
		//log.debug("Close");
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				log.error("Channel close {}", e);
			}/* finally {
				if (frames != null) {
					frames.clear();
					frames = null;
				}
			}*/
		}
		
	}

	public void setVideoCodecId(String videoCodecId) {
		this.videoCodecId = videoCodecId;
	}

	public void setAudioCodecId(String audioCodecId) {
		this.audioCodecId = audioCodecId;
	}

	public ITag readTagHeader() {
		return null;
	}

	@Override
	public KeyFrameMeta analyzeKeyFrames() {
		KeyFrameMeta result = new KeyFrameMeta();
		result.audioOnly = hasAudio && !hasVideo;
		result.duration = duration;
		if (result.audioOnly) {
			result.positions = new long[frames.size()];
			result.timestamps = new long[frames.size()];
			result.audioOnly = true;
			for (int i = 0; i < result.positions.length; i++) {
				frames.get(i).setKeyFrame(true);
				result.positions[i] = frames.get(i).getOffset();
				result.timestamps[i] = (int) Math.round(frames.get(i).getTime() * 1000.0);
			}
		} else {
			if (seekPoints != null) {
                int seekPointCount = seekPoints.size();
                result.positions = new long[seekPointCount];
                result.timestamps = new long[seekPointCount];
                for (int idx = 0; idx < seekPointCount; idx++) {
                    final Integer ts = seekPoints.get(idx);
                    result.positions[idx] = timePosMap.get(ts);
                    result.timestamps[idx] = ts;
                }
            } else {
                log.warn("Seek points array was null");
            }
		}
		return result;
	}

	@Override
	public ITagReader copy() {
		
		MP4Reader reader = new MP4Reader();
		/*
		reader.file = file;
		try {
			reader.fis = new FileInputStream(file);
			reader.channel = reader.fis.getChannel();
			// instance an iso file from mp4parser
			//reader.isoFile = new IsoFile(reader.channel);
			reader.isoFile = new IsoFile(file.getAbsolutePath());
		} catch (IOException e) {
			return null;
		}
		*/
		reader.timePosMap = timePosMap;
		reader.samplePosMap = samplePosMap;
		reader.hasVideo = hasVideo;
		reader.hasAudio = hasAudio;
		reader.videoCodecId = videoCodecId; 
		reader.audioCodecId = audioCodecId;
		reader.audioDecoderBytes = audioDecoderBytes;
		reader.videoDecoderBytes = videoDecoderBytes;
		reader.duration = duration;
		reader.timeScale = timeScale;
		reader.width = width;
		reader.height = height;
		reader.audioTimeScale = audioTimeScale;
		reader.audioChannels = audioChannels;
		reader.audioCodecType = audioCodecType;
		reader.videoSampleCount = videoSampleCount;
		reader.fps = fps;
		reader.videoTimeScale = videoTimeScale;
		reader.avcLevel = avcLevel;
		reader.avcProfile = avcProfile;
		reader.formattedDuration = formattedDuration;
		reader.mdatOffset = mdatOffset;
		reader.videoSamplesToChunks = videoSamplesToChunks;
		reader.audioSamplesToChunks = audioSamplesToChunks;
		reader.syncSamples = syncSamples; 
		reader.videoSamples = videoSamples;
		reader.audioSamples = audioSamples;
		reader.videoChunkOffsets = videoChunkOffsets;
		reader.audioChunkOffsets = audioChunkOffsets;
		reader.videoSampleDuration = videoSampleDuration;
		reader.audioSampleDuration = audioSampleDuration;
		reader.currentFrame = currentFrame;
		reader.prevFrameSize = prevFrameSize;
		reader.prevVideoTS = prevVideoTS;
		reader.frames = frames;
		reader.audioCount = audioCount;
		reader.videoCount = videoCount;
		reader.compositionTimes = compositionTimes;
		reader.firstTags = new LinkedList<>();
		reader.seekPoints = seekPoints;
		//reader.seekPoints = new LinkedList<>();
		return reader;
	}

}
