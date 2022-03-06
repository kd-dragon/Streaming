package com.kdy.bean.util.io.mp4;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

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
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.apple.AppleWaveBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.mdat.MediaDataBox;
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
import com.kdy.bean.util.io.Interface.ITagReader;

@Component
public class MP4ReaderFactory {
	private final Logger log  = LoggerFactory.getLogger(MP4ReaderFactory.class);
	
	@Cacheable(value="mp4ReaderCache", key="#vodFileSeq")
	public ITagReader initialize(String vodFileSeq, String vodFileFullPath, File file) throws IOException {
		log.debug("MP4ReaderFactory initialize #################");
		MP4Reader mp4Reader = new MP4Reader();
		
		IsoFile isoFile = new IsoFile(vodFileFullPath);
		decodeHeader(mp4Reader, isoFile);
		
		//MP4 Reader File Channel Connect
		mp4Reader.setVodFileChannel(file);
		mp4Reader.getAnalyzeFrames();
		mp4Reader.initFirstTags();
		
		isoFile.close();
		mp4Reader.close();
		
		return mp4Reader;
	}
	
	public void decodeHeader(MP4Reader mp4Reader, IsoFile isoFile) {
		
		List<CompositionTimeToSample.Entry> compositionTimes;
		long videoSampleCount;
		long timeScale;
		long duration;
		long mdatOffset;
		long[] videoSamples;
				
		// we want a moov and an mdat, anything else will throw the invalid file type error
		MovieBox moov = isoFile.getBoxes(MovieBox.class).get(0);
		// get the movie header
		MovieHeaderBox mvhd = moov.getMovieHeaderBox();
		// get the timescale and duration
		timeScale = mvhd.getTimescale();
		duration = mvhd.getDuration();
		
		mp4Reader.setTimeScale(timeScale);
		mp4Reader.setDuration(duration);
		// look at the tracks
		
		List<TrackBox> tracks = moov.getBoxes(TrackBox.class); // trak
		for (TrackBox trak : tracks) {
			
			TrackHeaderBox tkhd = trak.getTrackHeaderBox(); // tkhd
			if (tkhd != null && tkhd.getWidth() > 0) {
				mp4Reader.setWidth((int) tkhd.getWidth());
				mp4Reader.setHeight((int) tkhd.getHeight());
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
						
						mp4Reader.setHasVideo(true);
						if (scale > 0) {
							
							mp4Reader.setVideoTimeScale(scale * 1.0);
							//log.debug("Video time scale: {}", videoTimeScale);
						}
					} else if ("soun".equals(hdlrType)) {
						mp4Reader.setHasAudio(true);
						if (scale > 0) {
							mp4Reader.setAudioTimeScale(scale * 1.0);
							//log.debug("Audio time scale: {}", audioTimeScale);
						}
					} 
				}
				MediaInformationBox minf = mdia.getMediaInformationBox();
				if (minf != null) {
					AbstractMediaHeaderBox abs = minf.getMediaHeaderBox();
					if (abs != null) {
						if (abs instanceof SoundMediaHeaderBox) { // smhd
							//SoundMediaHeaderBox smhd = (SoundMediaHeaderBox) abs;
							//log.debug("Sound header atom found");
						} else if (abs instanceof VideoMediaHeaderBox) { // vmhd
							//VideoMediaHeaderBox vmhd = (VideoMediaHeaderBox) abs;
							//log.debug("Video header atom found");
							isAudio = true;
						} 
					} else {
						//log.debug("Null media header box");
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
						//log.debug("Sample entry type: {}", entry.getType());
						// determine if audio or video and process from there
						if (entry instanceof AudioSampleEntry) {
							processAudioBox(mp4Reader, stbl, (AudioSampleEntry) entry, scale);
						} else if (entry instanceof VisualSampleEntry) {
							processVideoBox(mp4Reader, stbl, (VisualSampleEntry) entry, scale);
						}
					} else {
						//log.debug("Sample entry was null");
						if (isVideo) {
							processVideoBox(mp4Reader, stbl, scale);
						} else if (isAudio) {
							processAudioBox(mp4Reader, stbl, scale);
						}
					}
				}
			}
		}
		//calculate FPS
		videoSampleCount = mp4Reader.getVideoSampleCount();
		compositionTimes = mp4Reader.getCompositionTimes();
		
		mp4Reader.setFps((videoSampleCount * timeScale) / (double) duration);
		//log.debug("FPS calc: ({} * {}) / {}", new Object[] { videoSampleCount, timeScale, duration });
		//log.info("FPS calc: ({} * {}) / {} ==> FPS {}",videoSampleCount, timeScale, duration, (videoSampleCount * timeScale) / (double) duration);

		List<MediaDataBox> mdats = isoFile.getBoxes(MediaDataBox.class);
		if (mdats != null && !mdats.isEmpty()) {
			//log.debug("mdat count: {}", mdats.size());
			MediaDataBox mdat = mdats.get(0);
			if (mdat != null) {
				//mdatOffset = mdat.getDataStartPosition();
				mdatOffset = mdat.getOffset();
				mp4Reader.setMdatOffset(mdatOffset);
			}
		}

		// detect whether or not this movie contains fragments first
		List<MovieFragmentBox> moofs = isoFile.getBoxes(MovieFragmentBox.class); // moof
		if (moofs != null && !moofs.isEmpty()) {
			//log.info("Movie contains {} framents", moofs.size());
			// fragmented = true;
			for (MovieFragmentBox moof : moofs) {
				
				if (compositionTimes == null) {
					compositionTimes = new ArrayList<CompositionTimeToSample.Entry>();
				}
				LinkedList<Integer> dataOffsets = new LinkedList<Integer>();
				LinkedList<Long> sampleSizes = new LinkedList<Long>();
				List<TrackRunBox> truns = moof.getTrackRunBoxes();
				for (TrackRunBox trun : truns) {
					//log.debug("trun - {}", trun);
					//videoSamplesToChunks
					dataOffsets.add(trun.getDataOffset());
					videoSampleCount += trun.getSampleCount();
					List<TrackRunBox.Entry> recs = trun.getEntries();
					for (TrackRunBox.Entry rec : recs) {
						CompositionTimeToSample.Entry ctts = new CompositionTimeToSample.Entry((int) trun.getSampleCount(), (int) rec.getSampleCompositionTimeOffset());
						compositionTimes.add(ctts);
						sampleSizes.add(rec.getSampleSize());
						mp4Reader.setVideoSampleDuration(rec.getSampleDuration());
					}
				}
				videoSamples = new long[sampleSizes.size()];
				for (int i = 0; i < videoSamples.length; i++) {
					videoSamples[i] = sampleSizes.remove();
				}
				long[] videoChunkOffsets = new long[dataOffsets.size()];
				for (int i = 0; i < videoChunkOffsets.length; i++) {
					videoChunkOffsets[i] = dataOffsets.remove();
				}
				
				mp4Reader.setVideoSamples(videoSamples);
				mp4Reader.setVideoChunkOffsets(videoChunkOffsets);
			}
		}
	}
	
	
	private void processVideoBox(MP4Reader mp4Reader, SampleTableBox stbl, VisualSampleEntry vse, long scale) {
		
		byte[] videoDecoderBytes;
		// get codec
		String codecName = vse.getType();
		//set the video codec here - may be avc1 or mp4v
		mp4Reader.setVideoCodecId(codecName);
		
		if ("avc1".equals(codecName)) {
			//AvcConfigurationBox avc1 = vse.getBoxes(AvcConfigurationBox.class).get(0);
			AvcConfigurationBox avc1 = vse.getBoxes(AvcConfigurationBox.class).get(0);
			mp4Reader.setAvcLevel(avc1.getAvcLevelIndication());
			mp4Reader.setAvcProfile(avc1.getAvcLevelIndication());
			//avcLevel = avc1.getAvcLevelIndication();
			//log.debug("AVC level: {}", avcLevel);
			//avcProfile = avc1.getAvcProfileIndication();
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
				mp4Reader.setVideoDecoderBytes(videoDecoderBytes);
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
							mp4Reader.setVideoDecoderBytes(videoDecoderBytes);
						}
					}
				}
			}
		} else {
			log.debug("Unrecognized video codec: {} compressor name: {}", codecName, vse.getCompressorname());
		}
		
		processVideoStbl(mp4Reader, stbl, scale);
	}
	
	/**
	 * Process the video information contained in the atoms.
	 * 
	 * @param stbl
	 * @param scale timescale
	 */
	private void processVideoBox(MP4Reader mp4Reader, SampleTableBox stbl, long scale) {
		processVideoStbl(mp4Reader, stbl, scale);
	}
	
	/**
	 * Process an stbl atom with containing video information.
	 * 
	 * @param stbl
	 * @param scale
	 */
	private void processVideoStbl(MP4Reader mp4Reader, SampleTableBox stbl, long scale) {
		
		// stsc - has Records
		SampleToChunkBox stsc = stbl.getSampleToChunkBox(); // stsc
		if (stsc != null) {
			//log.debug("Sample to chunk atom found");
			//videoSamplesToChunks = stsc.getEntries();
			mp4Reader.setVideoSamplesToChunks(stsc.getEntries());
			//log.debug("Video samples to chunks: {}", videoSamplesToChunks.size());
		}
		// stsz - has Samples
		SampleSizeBox stsz = stbl.getSampleSizeBox(); // stsz
		if (stsz != null) {
			//log.debug("Sample size atom found");
			//videoSamples = stsz.getSampleSizes();
			mp4Reader.setVideoSamples(stsz.getSampleSizes());
			// if sample size is 0 then the table must be checked due to variable sample sizes
			//log.debug("Sample size: {}", stsz.getSampleSize());
			mp4Reader.setVideoSampleCount(stsz.getSampleCount());
			//videoSampleCount = stsz.getSampleCount();
			//log.debug("Sample count: {}", videoSampleCount);
		}
		// stco - has Chunks
		ChunkOffsetBox stco = stbl.getChunkOffsetBox(); // stco / co64
		if (stco != null) {
			//log.debug("Chunk offset atom found");
			mp4Reader.setVideoChunkOffsets(stco.getChunkOffsets());
			//videoChunkOffsets = stco.getChunkOffsets();
			//log.debug("Chunk count: {}", videoChunkOffsets.length);
		} else {
			// co64 - has Chunks
			ChunkOffset64BitBox co64 = stbl.getBoxes(ChunkOffset64BitBox.class).get(0);
			if (co64 != null) {
				//log.debug("Chunk offset (64) atom found");
				//videoChunkOffsets = co64.getChunkOffsets();
				mp4Reader.setVideoChunkOffsets(co64.getChunkOffsets());
				//log.debug("Chunk count: {}", videoChunkOffsets.length);
			}
		}
		// stss - has Sync - no sync means all samples are keyframes
		SyncSampleBox stss = stbl.getSyncSampleBox(); // stss
		if (stss != null) {
			//log.debug("Sync sample atom found");
			mp4Reader.setSyncSamples(stss.getSampleNumber());
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
				//videoSampleDuration = rec.getDelta();
				mp4Reader.setVideoSampleDuration(rec.getDelta());
			}
		}
		// ctts - (composition) time to sample
		CompositionTimeToSample ctts = stbl.getCompositionTimeToSample(); // ctts
		if (ctts != null) {
			//log.debug("Composition time to sample atom found");
			//compositionTimes = ctts.getEntries();
			mp4Reader.setCompositionTimes( ctts.getEntries());
		}


	}
	
	private void processAudioBox(MP4Reader mp4Reader, SampleTableBox stbl, AudioSampleEntry ase, long scale) {
		
		byte[] audioDecoderBytes;
		int audioCodecType = 1;
		// get codec
		String codecName = ase.getType();
		// set the audio codec here - may be mp4a or...
		mp4Reader.setAudioCodecId(codecName);
		//log.debug("Sample size: {}", ase.getSampleSize());
		long ats = ase.getSampleRate();
		// skip invalid audio time scale
		if (ats > 0) {
			mp4Reader.setAudioTimeScale(ats * 1.0);
		}
		//log.debug("Sample rate (audio time scale): {}", audioTimeScale);
		mp4Reader.setAudioChannels(ase.getChannelCount());
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
						mp4Reader.setAudioDecoderBytes(audioDecoderBytes);
						
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
								mp4Reader.setAudioCodecId("mp3");
								break;
						}
						mp4Reader.setAudioCodecType(audioCodecType);
						//log.debug("Audio coder type: {} {} id: {}", new Object[] { audioCoderType, Integer.toBinaryString(audioCoderType), audioCodecId });
					} 
				} else {
					log.warn("No ES descriptor found");
				}
			}
		} else {
			log.warn("Audio sample entry had no descriptor");
		}
		
		processAudioStbl(mp4Reader, stbl, scale);
	}
	
	/**
	 * Process the audio information contained in the atoms.
	 * 
	 * @param stbl
	 * @param scale timescale
	 */
	private void processAudioBox(MP4Reader mp4Reader, SampleTableBox stbl, long scale) {

		processAudioStbl(mp4Reader, stbl, scale);
	}
	
	private void processAudioStbl(MP4Reader mp4Reader, SampleTableBox stbl, long scale) {
		//stsc - has Records
		SampleToChunkBox stsc = stbl.getSampleToChunkBox(); // stsc
		if (stsc != null) {
			//audioSamplesToChunks = stsc.getEntries();
			mp4Reader.setAudioSamplesToChunks(stsc.getEntries());
		}
		//stsz - has Samples
		SampleSizeBox stsz = stbl.getSampleSizeBox(); // stsz
		if (stsz != null) {
			//log.debug("Sample size atom found");
			//audioSamples = stsz.getSampleSizes();
			mp4Reader.setAudioSamples(stsz.getSampleSizes());
			//log.debug("Samples: {}", audioSamples.length);
			// if sample size is 0 then the table must be checked due to variable sample sizes
			mp4Reader.setAudioSampleSize(stsz.getSampleSize());
			//audioSampleSize = stsz.getSampleSize();
		}
		//stco - has Chunks
		ChunkOffsetBox stco = stbl.getChunkOffsetBox(); // stco / co64
		if (stco != null) {
			//log.debug("Chunk offset atom found");
			//audioChunkOffsets = stco.getChunkOffsets();
			mp4Reader.setAudioChunkOffsets(stco.getChunkOffsets());
			//log.debug("Chunk count: {}", audioChunkOffsets.length);
		} else {
			//co64 - has Chunks
			ChunkOffset64BitBox co64 = stbl.getBoxes(ChunkOffset64BitBox.class).get(0);
			if (co64 != null) {
				//log.debug("Chunk offset (64) atom found");
				mp4Reader.setAudioChunkOffsets(co64.getChunkOffsets());
				//audioChunkOffsets = co64.getChunkOffsets();
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
				mp4Reader.setAudioSampleDuration(rec.getDelta());
				//audioSampleDuration = rec.getDelta();
			}
		}	
	}
}
