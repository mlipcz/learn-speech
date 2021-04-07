package pl.modulo;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClientBuilder;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Translator {

	private final AmazonTranslate client;

	public Translator(Regions regions) {
		AWSCredentials credentials = getCredentialsProvider().getCredentials();

		client = AmazonTranslateClientBuilder
				.standard()
				.withRegion(regions)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();
	}

	public String translate(String text, String sourceLang, String targetLang) {
		TranslateTextRequest request = new TranslateTextRequest()
				.withText(text)
				.withSourceLanguageCode(sourceLang)
				.withTargetLanguageCode(targetLang);
		try {
			TranslateTextResult result = client.translateText(request);
			log.info("Translate {} from {} to {}, result: {}", text, sourceLang, targetLang, result.getTranslatedText());
			return result.getTranslatedText();
		} catch (Exception e) {
			log.error("Amazon Translate Exception:", e);
			return null;
		}
	}

	private AWSCredentialsProvider getCredentialsProvider() {
		return (new AWSCredentialsProviderChain(new EnvironmentVariableCredentialsProvider(), new SystemPropertiesCredentialsProvider(), new ProfileCredentialsProvider()) {
			public AWSCredentials getCredentials() {
				try {
					return super.getCredentials();
				} catch (AmazonClientException e) {
					log.error("Amazon Client Exception:", e);
					return null;
				}
			}
		});
	}
}
