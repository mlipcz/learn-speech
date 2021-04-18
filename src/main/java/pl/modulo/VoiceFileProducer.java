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
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VoiceFileProducer {

	private final Properties properties;
	private final Translator translator;
	private final SentenceProducer sentenceProducer;
	private final AmazonPollyClient polly;
	private final Voice voicePL, voiceZH;
	private final String outputDirectory, outputWav;

	public VoiceFileProducer(Properties properties) throws IOException {
		this.properties = properties;
		Regions regions = Regions.fromName(properties.getRegion());
		this.translator = new Translator(regions);

		// TODO don't use deprecated methods
		polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), new ClientConfiguration());
		polly.setRegion(Region.getRegion(regions));
		DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest().withLanguageCode(properties.getDestLanguage());

		DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
		if (describeVoicesResult.getVoices().isEmpty()) {
			throw new RuntimeException("No voices found for destination language " + properties.getDestLanguage());
		}
		voiceZH = describeVoicesResult.getVoices().get(0);

		describeVoicesRequest = new DescribeVoicesRequest().withLanguageCode(properties.getSourceLanguage());
		describeVoicesResult = polly.describeVoices(describeVoicesRequest);
		if (describeVoicesResult.getVoices().isEmpty()) {
			throw new RuntimeException("No voices found for source language " + properties.getSourceLanguage());
		}
		voicePL = describeVoicesResult.getVoices().get(0);

		this.outputDirectory = Files.createTempDirectory("VoiceFileProducer").toString();
		this.outputWav = this.outputDirectory + "/A000.wav";

		this.sentenceProducer = new SentenceProducer();
	}

	public void generateVoiceFile() throws JavaLayerException, IOException, UnsupportedAudioFileException, EncoderException {
		List<String> filenames = generateWavFiles();
		mergeWavFiles(filenames);
		deleteWavFiles(filenames);
		toMp3();
		System.out.println("Generated file: " + this.outputWav);
	}

	private List<String> generateWavFiles() throws IOException, JavaLayerException {
		Converter mp3ToWavConverter = new Converter();
		List<String> filenames = new ArrayList<>();
		String longDelay = "<break time=\"" + properties.getLongDelay() + "\"/>";
		String shortDelay = "<break time=\"" + properties.getShortDelay() + "\"/>";
		String sourceLangShort = extractLowercase(this.properties.getSourceLanguage());
		String destLangShort = extractLowercase(this.properties.getDestLanguage());
		String modelLangShort = extractLowercase(this.properties.getModelLanguage());
		if (destLangShort.equals("cmn")) // TODO make this mapping more general
			destLangShort = "zh";
		for (int i = 0; i <= properties.getSentenceCount(); i++) {
			String en = sentenceProducer.nextSentence();
			String pl = translator.translate(en, modelLangShort, sourceLangShort);
			String zh = translator.translate(en, modelLangShort, destLangShort);
			InputStream speechStreamZH = synthesize("<speak><prosody rate=\"" + properties.getVoiceSpeed() + "%\">" + zh + "</prosody>" + shortDelay + "</speak>", OutputFormat.Mp3, voiceZH);
			InputStream speechStreamPL = synthesize("<speak>" + pl + longDelay + "</speak>", OutputFormat.Mp3, voicePL);
			String zhName = outputDirectory + "A" + i + "zh.mp3";
			Files.copy(speechStreamZH, new File(zhName).toPath(), StandardCopyOption.REPLACE_EXISTING);
			String plName = outputDirectory + "A" + i + "pl.mp3";
			Files.copy(speechStreamPL, new File(plName).toPath(), StandardCopyOption.REPLACE_EXISTING);
			mp3ToWavConverter.convert(plName, plName + ".wav");
			mp3ToWavConverter.convert(zhName, zhName + ".wav");

			filenames.add(plName + ".wav");
			filenames.add(zhName + ".wav");

			new File(plName).delete();
			new File(zhName).delete();
		}
		return filenames;
	}

	private String extractLowercase(String text) {
		return text.replaceAll("[^a-z]", "");
	}

	private void mergeWavFiles(List<String> filenames) throws UnsupportedAudioFileException, IOException {
		AudioInputStream audio1 = AudioSystem.getAudioInputStream(new File(filenames.get(0)));
		AudioInputStream audio2 = AudioSystem.getAudioInputStream(new File(filenames.get(1)));

		AudioInputStream audioBuild = new AudioInputStream(new SequenceInputStream(audio1, audio2), audio1.getFormat(), audio1.getFrameLength() + audio2.getFrameLength());

		for (int i = 2; i < filenames.size(); i++) {
			audio2 = AudioSystem.getAudioInputStream(new File(filenames.get(i)));
			audioBuild = new AudioInputStream(new SequenceInputStream(audioBuild, audio2), audioBuild.getFormat(), audioBuild.getFrameLength() + audio2.getFrameLength());
		}

		AudioSystem.write(audioBuild, AudioFileFormat.Type.WAVE, new File(outputWav));
	}

	private void deleteWavFiles(List<String> filenames) {
		filenames.forEach(s -> new File(s).delete());
		filenames.clear();
	}

	private InputStream synthesize(String text, OutputFormat format, Voice voice) {
		SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(text).withTextType(TextType.Ssml).withVoiceId(voice.getId()).withOutputFormat(format);
		SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);

		return synthRes.getAudioStream();
	}

	private void toMp3() throws EncoderException {
		File source = new File(outputWav);
		File target = new File(properties.getOutputSoundFile());
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
		source.deleteOnExit();
	}
}
