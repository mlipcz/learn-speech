package pl.modulo;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.*;
import javazoom.jl.converter.Converter;
import javazoom.jl.decoder.JavaLayerException;
import lombok.extern.slf4j.Slf4j;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Slf4j
public class VoiceFileProducer {

	final static String[] T2 = {"my", "your", "his", "our", "their"};
	final static String[] T3 = {"father", "mother", "younger sister", "younger brother", "older sister", "older brother", "son", "daughter"};
	private final Properties properties;
	private final Translator translator;
	private final AmazonPollyClient polly;
	private final Voice voicePL, voiceZH;

	public VoiceFileProducer(Properties properties) {
		this.properties = properties;
		Regions regions = Regions.fromName(properties.getRegion());
		this.translator = new Translator(regions);

		polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), new ClientConfiguration());
		polly.setRegion(Region.getRegion(regions));
		DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest().withLanguageCode(properties.getDestLanguage());

		DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
		voiceZH = describeVoicesResult.getVoices().get(0);

		describeVoicesRequest = new DescribeVoicesRequest().withLanguageCode(properties.getSourceLanguage());
		describeVoicesResult = polly.describeVoices(describeVoicesRequest);
		voicePL = describeVoicesResult.getVoices().get(0);
	}

	public void generateVoiceFile() throws JavaLayerException, IOException, UnsupportedAudioFileException, EncoderException {
		Converter myConverter = new Converter();
		Random rnd = new Random();
		List<String> filenames = new ArrayList<>();
		if (!new File("d:\\TMP\\silence.wav").exists()) {
			myConverter.convert("d:\\TMP\\silence.mp3", "d:\\TMP\\silence.wav");
		}
		for (int i = 0; i <= properties.getSentenceCount(); i++) {
			String en = T2[rnd.nextInt(T2.length)] + " " + T3[rnd.nextInt(T3.length)] + "'s birthday is on " + (1 + rnd.nextInt(25)) + " " + new DateFormatSymbols(Locale.ENGLISH).getMonths()[rnd.nextInt(12)] + ".";
			String pl = translator.translate(en, "en", "pl");
			String zh = translator.translate(en, "en", "zh");
			InputStream speechStreamZH = synthesize("<speak><prosody rate=\"" + properties.getVoiceSpeed() + "%\">" + zh + "</prosody></speak>", OutputFormat.Mp3, voiceZH);
			InputStream speechStreamPL = synthesize("<speak>" + pl + "</speak>", OutputFormat.Mp3, voicePL);
			String zhName = "D:/tmp/A" + i + "zh.mp3";
			Files.copy(speechStreamZH, new File(zhName).toPath(), StandardCopyOption.REPLACE_EXISTING);
			String plName = "D:/tmp/A" + i + "pl.mp3";
			Files.copy(speechStreamPL, new File(plName).toPath(), StandardCopyOption.REPLACE_EXISTING);
			myConverter.convert(plName, plName + ".wav");
			myConverter.convert(zhName, zhName + ".wav");

			filenames.add(plName + ".wav");
			filenames.add("d:\\TMP\\silence.wav");
			filenames.add(zhName + ".wav");
		}

		mergeWavFiles(filenames);
		toMp3();
	}

	private void mergeWavFiles(List<String> filenames) throws UnsupportedAudioFileException, IOException {
		AudioInputStream audio1 = AudioSystem.getAudioInputStream(new File(filenames.get(0)));
		AudioInputStream audio2 = AudioSystem.getAudioInputStream(new File(filenames.get(1)));

		AudioInputStream audioBuild = new AudioInputStream(new SequenceInputStream(audio1, audio2), audio1.getFormat(), audio1.getFrameLength() + audio2.getFrameLength());

		for (int i = 2; i < filenames.size(); i++) {
			audio2 = AudioSystem.getAudioInputStream(new File(filenames.get(i)));
			audioBuild = new AudioInputStream(new SequenceInputStream(audioBuild, audio2), audioBuild.getFormat(), audioBuild.getFrameLength() + audio2.getFrameLength());
		}

		AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, new File("d:/tmp/A000.wav"));
	}

	public InputStream synthesize(String text, OutputFormat format, Voice voice) {
		SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(text).withTextType(TextType.Ssml).withVoiceId(voice.getId()).withOutputFormat(format);
		SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);

		return synthRes.getAudioStream();
	}

	private void toMp3() throws EncoderException {
		File source = new File("d:/tmp/A000.wav");
		File target = new File("d:/tmp/A000.mp3");
		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("libmp3lame");
		audio.setBitRate(128000);
		audio.setChannels(1);
		audio.setSamplingRate(44100);
		EncodingAttributes attrs = new EncodingAttributes();
		attrs.setOutputFormat("mp3");
		attrs.setAudioAttributes(audio);
		Encoder encoder = new Encoder();
		encoder.encode(new MultimediaObject(source), target, attrs);
	}
}
