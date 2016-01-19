package com.tdt.aws.servlet;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

import com.amazonaws.util.Base16;
import com.amazonaws.util.Base64;
import com.amazonaws.util.DateUtils;

public class DynamodbService {

	// you have to change to your own table name and region here
	public static final String LOGIN_TABLE = "users";
	public static String REGION = "us-east-1";
	// put your access key id here
	public static String AWS_ACCESS_KEY = "";
	// put your secret key here
	public static String AWS_SETCRET_KEY = "";

	public static HttpPost buildGetItemHeaders(String table, String region)
			throws Exception {

		String getItem = "DynamoDB_20120810.GetItem";
		String date = new SimpleDateFormat("YYYYMMDD").format(new Date());
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(AWS_SETCRET_KEY.getBytes("UTF-8")); // Change this to "UTF-16" if needed
		byte[] digest = md.digest();

		String nowAsISO = DateUtils.formatISO8601Date(new Date());

		String host = "dynamodb." + region + ".amazonaws.com";
		System.err.println(host);
		System.err.println(date);
		Header header1 = new BasicHeader("host", host);
		Header header2 = new BasicHeader("x-amz-date", nowAsISO);
		Header header3 = new BasicHeader("x-amz-target", getItem);
		Header header4 = new BasicHeader("content-type",
				"application/x-amz-json-1.0");
		Header header6 = new BasicHeader("connection", "Keep-Alive");
		String signature = new String(getSignatureKey(new String(Base64.encode(digest)), date, region, "iam"), "UTF8").toLowerCase();
		Header header7 = new BasicHeader("Authorization",
				"AWS4-HMAC-SHA256 Credential=" + AWS_ACCESS_KEY + "/" + date
						+ "/" + region
						+ "/dynamodb/aws4_request,SignedHeaders=host;" + date
						+ ";" + getItem + ",Signature=" + signature);
		System.err.println(signature);
		HttpPost postRequest = new HttpPost("http://dynamodb." + region
				+ ".amazonaws.com");
		postRequest.setHeaders(new Header[] { header1, header2, header3,
				header4, header6, header7 });
		return postRequest;
	}

	public static HttpPost connect() throws Exception {
		return buildGetItemHeaders(LOGIN_TABLE, REGION);
	}

	public static boolean checkLogin(String email, String password)
			throws Exception {
		// TODO Auto-generated method stub
		HttpPost httpPost = connect();

		// buiding json object
		JSONObject jsonObject = new JSONObject();
		// email column name - your table's primary key, you have to change to
		// your own
		String emailColumnName = "email";
		jsonObject.put("TableName", LOGIN_TABLE).put(
				"Key",
				new JSONObject().put(emailColumnName,
						new JSONObject().put("S", email)));
		// creating request body
		HttpClient httpClient = HttpClientBuilder.create().build();
		httpPost.setEntity(new StringEntity(jsonObject.toString()));

		// execute request and get response
		HttpResponse res = httpClient.execute(httpPost);
		String s = IOUtils.toString(res.getEntity().getContent());
		System.out.println(s);

		JSONObject responseJson = new JSONObject(s);
		// I assume you have a password column to contain password
		String passwordInDB = responseJson.getJSONObject("Item").getString(
				"password");

		return passwordInDB.equals(password);
	}

	public static void main(String[] args) throws Exception {
		checkLogin("khoi@gmail.com", "123");
	}
	
	static byte[] HmacSHA256(String data, byte[] key) throws Exception  {
	     String algorithm="HmacSHA256";
	     Mac mac = Mac.getInstance(algorithm);
	     mac.init(new SecretKeySpec(key, algorithm));
	     return mac.doFinal(data.getBytes("UTF8"));
	}

	static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception  {
	     byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
	     byte[] kDate    = HmacSHA256(dateStamp, kSecret);
	     byte[] kRegion  = HmacSHA256(regionName, kDate);
	     byte[] kService = HmacSHA256(serviceName, kRegion);
	     byte[] kSigning = HmacSHA256("aws4_request", kService);
	     return kSigning;
	}
}
