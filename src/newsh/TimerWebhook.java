package newsh;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import haven.Config;


public class TimerWebhook {

	public static void updateTimerBot(String timerName, long timerDuration, String command) {
		JsonParser parser = new JsonParser();
		String timerMobileNotifications = Config.loadFile("timerMobileNotifications.json");

		Object obj = parser.parse(timerMobileNotifications);

		JsonObject jsonObject = (JsonObject) obj;

		JsonElement telegramChatIDElement = jsonObject.get("telegram");
		JsonElement webhookUrlElement = jsonObject.get("webhook");
		String telegramChatID = telegramChatIDElement.getAsString();
		String webhookUrl= webhookUrlElement.getAsString();
		if(!webhookUrl.isEmpty() && !telegramChatID.isEmpty()) { //Only try to send data if user has set a webhook.
			String jsonString = "{\"timerName\":\"" + timerName + "\",\"duration\":\"" + timerDuration + "\",\"chat_id\":\"" + telegramChatID + "\",\"command\":\"" + command + "\"}"; // build json String to send to script
			try {
				jsonString = URLEncoder.encode(jsonString, "UTF-8"); // Url encode, so special characters used in name don't break url
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			HttpClient httpClient = HttpClientBuilder.create().build();
			try {
				HttpPost request = null;
				request = new HttpPost(webhookUrl);  //This needs to be quiried from some config.ini later on
				StringEntity params = new StringEntity("message=" + jsonString);
				request.addHeader("content-type", "application/x-www-form-urlencoded");
				request.addHeader("charset", "utf-8");
				request.setEntity(params);
				httpClient.execute(request); // Send json data to script.
			} catch (Exception ex) {ex.printStackTrace();}
		} else {return; }
	}

}

