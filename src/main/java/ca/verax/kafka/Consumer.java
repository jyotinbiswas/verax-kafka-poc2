package ca.verax.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;

/**
 * This program reads messages from two topics. Messages on "fast-messages" are
 * analyzed to estimate latency (assuming clock synchronization between producer
 * and consumer).
 * <p/>
 * Whenever a message is received on "slow-messages", the stats are dumped.
 */
public class Consumer {
	public static void main(String[] args) throws IOException {
		// set up house-keeping
		ObjectMapper mapper = new ObjectMapper();

		// and the consumer
		KafkaConsumer<String, String> consumer;
		try (InputStream props = Resources.getResource("consumer.props").openStream()) {
			Properties properties = new Properties();
			properties.load(props);
			if (properties.getProperty("group.id") == null) {
				properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
			}
			consumer = new KafkaConsumer<>(properties);
		}
		consumer.subscribe(Arrays.asList("stock-quotes"));
		int timeouts = 0;
		// noinspection InfiniteLoopStatement
		while (true) {
			// read records with a short timeout. If we time out, we don't
			// really care.
			ConsumerRecords<String, String> records = consumer.poll(200);
			if (records.count() == 0) {
				timeouts++;
			} else {
				System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
				timeouts = 0;
			}			
			for (ConsumerRecord<String, String> record : records) {
				JsonNode msg = mapper.readTree(record.value());
				String exchange = msg.get("exchange").asText();
                String ticker = msg.get("ticker").asText();
				String tradeDate = msg.get("tradeDate").asText();
				float open = msg.get("open").floatValue();
				float high = msg.get("high").floatValue();
				float low = msg.get("low").floatValue();
				float close = msg.get("close").floatValue();
				float volume = msg.get("volume").floatValue();
				System.out.println("exchange = " + exchange + "ticker = " + ticker + " trade date = " + tradeDate +
                        " open = " + open + " high = " + high + " low = " + low + " close = "
						+ close + " lastTrade = " + volume);
			}
		}
	}
}