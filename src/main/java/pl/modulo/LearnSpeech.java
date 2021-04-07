package pl.modulo;

public class LearnSpeech {
	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		VoiceFileProducer voiceFileProducer = new VoiceFileProducer(properties);
		voiceFileProducer.generateVoiceFile();
	}
}
