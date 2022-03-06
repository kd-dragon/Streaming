package com.kdy.bean.util.media;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kdy.bean.util.io.amf.Input;
import com.kdy.bean.util.io.object.Deserializer;
import com.kdy.bean.util.media.Interface.IMeta;
import com.kdy.bean.util.media.Interface.IMetaCue;
import com.kdy.bean.util.media.Interface.IMetaData;
import com.kdy.bean.util.media.Interface.IMetaService;

public class MetaService implements IMetaService {

	protected static Logger log = LoggerFactory.getLogger(MetaService.class);

	/**
	 * Source file
	 */
	File file;

	/**
	 * MetaService constructor
	 */
	public MetaService() {
		super();
	}

	public MetaService(File poFil) {
		this();
		this.file = poFil;
	}

	/**
	 * Merges the two Meta objects
	 *
	 * @param metaData1 First metadata object
	 * @param metaData2 Second metadata object
	 *
	 * @return Merged metadata
	 */
	@SuppressWarnings({"unchecked"})
	public static IMeta mergeMeta(IMetaData<?, ?> metaData1, IMetaData<?, ?> metaData2) {
		//walk the entries and merge them
		//1. higher number values trump lower ones
		//2. true considered higher than false
		//3. strings are not replaced
		Map<String, Object> map1 = ((Map<String, Object>) metaData1);
		Set<Entry<String, Object>> set1 = map1.entrySet();
		Map<String, Object> map2 = ((Map<String, Object>) metaData2);
		Set<Entry<String, Object>> set2 = map2.entrySet();
		//map to hold updates / replacements
		Map<String, Object> rep = new HashMap<String, Object>();
		//loop to update common elements
		for (Entry<String, Object> entry1 : set1) {
			String key1 = entry1.getKey();
			if (map2.containsKey(key1)) {
				Object value1 = map1.get(key1);
				Object value2 = map2.get(key1);
				//we dont replace strings
				//check numbers
				if (value1 instanceof Double) {
					if (Double.valueOf(value1.toString()).doubleValue() < Double.valueOf(value2.toString()).doubleValue()) {
						rep.put(key1, value2);
					}
				} else if (value1 instanceof Integer) {
					if (Integer.valueOf(value1.toString()).intValue() < Integer.valueOf(value2.toString()).intValue()) {
						rep.put(key1, value2);
					}
				} else if (value1 instanceof Long) {
					if (Long.valueOf(value1.toString()).longValue() < Long.valueOf(value2.toString()).longValue()) {
						rep.put(key1, value2);
					}
				}
				//check boolean
				if (value1 instanceof Boolean) {
					//consider true > false
					if (!Boolean.valueOf(value1.toString()) && Boolean.valueOf(value2.toString())) {
						rep.put(key1, value2);
					}
				}
			}
		}
		//remove all changed
		set1.removeAll(rep.entrySet());
		//add the updates
		set1.addAll(rep.entrySet());
		//perform a union / adds all elements missing from set1
		set1.addAll(set2);
		//return the original object with merges
		return metaData1;
	}

	@SuppressWarnings("unused")
	private int getTimeInMilliseconds(IMetaCue metaCue) {
		return (int) (metaCue.getTime() * 1000.00);
	}


	/**
	 * {@inheritDoc}
	 */
	public void writeMetaCue() {

	}

	/**
	 * @return Returns the file.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file The file to set.
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * {@inheritDoc}
	 */
	// TODO need to fix
	public MetaData<?, ?> readMetaData(IoBuffer buffer) {
		MetaData<?, ?> retMeta  = new MetaData<String, Object>();
		Input input = new Input(buffer);
		String metaType = Deserializer.deserialize(input, String.class);
		log.debug("Metadata type: {}", metaType);
		Map<String, ?> m = Deserializer.deserialize(input, Map.class);
		retMeta.putAll(m);
		return retMeta;
	}

	/**
	 * {@inheritDoc}
	 */
	public IMetaCue[] readMetaCue() {
		return null;
	}

	@Override
	public void write(IMetaData<?, ?> meta) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeMetaData(IMetaData<?, ?> metaData) {
		// TODO Auto-generated method stub
		
	}


}
