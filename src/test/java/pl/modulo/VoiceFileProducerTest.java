package pl.modulo;

import org.junit.Test;

public class VoiceFileProducerTest {

	@Test
	public void test1() {
		String s = "aBcD-eFg";
		s = s.replaceAll("[^a-z]", "");
		assert (s.equals("aceg"));
	}
}
