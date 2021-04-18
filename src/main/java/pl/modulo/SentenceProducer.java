package pl.modulo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

public class SentenceProducer {

	private final List<String> models;
	private final Random rnd;

	public SentenceProducer() {
		InputStream sentencesInputStream = getClass().getClassLoader().getResourceAsStream("sentences.txt");
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sentencesInputStream));
		models = bufferedReader.lines().collect(Collectors.toList());
		rnd = new Random();
	}

	public String nextSentence() {
		String modelSentence = models.get(rnd.nextInt(models.size()));
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < modelSentence.length()) {
			int j = modelSentence.indexOf("(", i);
			if (j < 0) {
				sb.append(modelSentence.substring(i));
				break;
			}
			int k = modelSentence.indexOf(")", j);
			if (k < 0)
				throw new RuntimeException("opening bracket found, missing closing one");
			String pickedItem = modelSentence.substring(j + 1, k);
			sb.append(modelSentence, i, j).append(getRandomItem(pickedItem));
			i = k + 1;
		}
		return sb.toString();
	}

	private String getRandomItem(String specialText) {
		switch (specialText) {
			case "MONTH":
				return new DateFormatSymbols(Locale.ENGLISH).getMonths()[rnd.nextInt(12)];
		}
		if (specialText.contains("-")) {
			int minus = specialText.indexOf("-");
			int min = Integer.parseInt(specialText.substring(0, minus));
			int max = Integer.parseInt(specialText.substring(minus + 1));
			return String.valueOf(min + rnd.nextInt(max - min + 1));
		}
		String[] items = specialText.split("\\|");
		return items[rnd.nextInt(items.length)];
	}
}
