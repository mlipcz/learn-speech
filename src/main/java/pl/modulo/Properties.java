package pl.modulo;

import lombok.Getter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Getter
public class Properties {
	private final String region, modelLanguage, sourceLanguage, destLanguage;
	private final int sentenceCount, voiceSpeed;

	public Properties() throws IOException {
		java.util.Properties prop = new java.util.Properties();
		String propFileName = "config.properties";

		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

		if (inputStream != null) {
			prop.load(inputStream);
		} else {
			throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
		}

		this.region = prop.getProperty("region");
		this.modelLanguage = prop.getProperty("modelLanguage");
		this.sourceLanguage = prop.getProperty("sourceLanguage");
		this.destLanguage = prop.getProperty("destLanguage");
		this.sentenceCount = Integer.parseInt(prop.getProperty("sentenceCount"));
		this.voiceSpeed = Integer.parseInt(prop.getProperty("voiceSpeed"));
	}
}
