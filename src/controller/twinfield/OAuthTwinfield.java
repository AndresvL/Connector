package controller.twinfield;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import object.Settings;
import object.Token;
import object.twinfield.Search;

import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// OAuth 1.0
public class OAuthTwinfield extends Authenticate {
	// Does this still work if there're multiple users using this methode?
	@SuppressWarnings("unused")
	private static String callbackConfirmed = null;
	private static Token token = null;
	private String callback = System.getenv("CALLBACK");
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	
	public Token getTempToken(String consumerKey, String consumerSecret, String softwareToken, String softwareName,
			Boolean newLogin) throws ClientProtocolException, IOException, SQLException {
		// Check if user has the accessToken stored in the database
		Token accessToken = TokenDAO.getToken(softwareToken, softwareName);
		if (accessToken == null || newLogin) {
			token = new Token();
			token.setConsumerToken(consumerKey);
			token.setConsumerSecret(consumerSecret);
			token.setSoftwareToken(softwareToken);
			token.setSoftwareName(softwareName);
			String uri = "https://login.twinfield.com/oauth/initiate.aspx";
			// Change to WorkOrder host
			// action is verify.do
			CloseableHttpClient httpclient;
			httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(uri);
			
			// Build temporary token request
			// Set consumer_key, consumer_secret_key and callback
			StringBuilder headerReq = new StringBuilder();
			headerReq.append("OAuth ");
			headerReq.append("realm=\"Twinfield\", ");
			headerReq.append("oauth_consumer_key=\"" + consumerKey + "\", ");
			headerReq.append("oauth_signature_method=\"PLAINTEXT\", ");
			headerReq.append("oauth_timestamp=\"\", ");
			headerReq.append("oauth_nonce=\"\", ");
			headerReq.append("oauth_callback=\"" + callback + "\", ");
			headerReq.append("oauth_signature=\"" + consumerSecret + "&\"");
			
			httpGet.addHeader("Authorization", headerReq.toString());
			CloseableHttpResponse response = httpclient.execute(httpGet);
			
			try {
				HttpEntity entity = response.getEntity();
				String responseString = EntityUtils.toString(entity);
				String params[] = responseString.split("&");
				Map<String, String> map = new HashMap<String, String>();
				for (String param : params) {
					String name = param.split("=")[0];
					String value = param.split("=")[1];
					map.put(name, value);
				}
				// Get temporary token and secret_temporary token
				for (Entry<String, String> entry : map.entrySet()) {
					if (entry.getKey().equals("oauth_token")) {
						token.setTempToken(entry.getValue());
					}
					if (entry.getKey().equals("oauth_token_secret")) {
						token.setTempSecret(entry.getValue());
					}
					if (entry.getKey().equals("oauth_callback_confirmed")) {
						callbackConfirmed = entry.getValue();// Value is always
																// "true".
					}
				}
			} finally {
				response.close();
			}
			return token;
		} else {
			return accessToken;
		}
	}
	
	public Token getAccessToken(String tempToken, String verifyToken, String softwareName)
			throws ClientProtocolException, IOException {
		token.setTempToken(tempToken);
		token.setVerifyToken(verifyToken);
		token.setSoftwareName(softwareName);
		
		String uri = "https://login.twinfield.com/oauth/finalize.aspx";
		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(uri);
		
		// //Build access token request
		// //Set consumer_key, consumer_secret_key, temporary_secret_key,
		// temporary_key and verify_key
		StringBuilder headerReq = new StringBuilder();
		headerReq.append("OAuth ");
		headerReq.append("realm=\"Twinfield\", ");
		headerReq.append("oauth_consumer_key=\"" + token.getConsumerToken() + "\", ");
		headerReq.append("oauth_signature_method=\"PLAINTEXT\", ");
		headerReq.append("oauth_signature=\"" + token.getConsumerSecret() + "&" + token.getTempSecret() + "\", ");
		headerReq.append("oauth_token=\"" + token.getTempToken() + "\", ");
		headerReq.append("oauth_verifier=\"" + verifyToken + "\"");
		httpGet.addHeader("Authorization", headerReq.toString());
		CloseableHttpResponse response = httpclient.execute(httpGet);
		try {
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity);
			String params[] = responseString.split("&");
			Map<String, String> map = new HashMap<String, String>();
			for (String param : params) {
				String name = param.split("=")[0];
				String value = param.split("=")[1];
				map.put(name, value);
			}
			// Get access token and secret_access token
			for (Entry<String, String> entry : map.entrySet()) {
				if (entry.getKey().equals("oauth_token")) {
					token.setAccessToken(entry.getValue());
				}
				if (entry.getKey().equals("oauth_token_secret")) {
					token.setAccessSecret(entry.getValue());
				}
			}
		} finally {
			response.close();
		}
		try {
			TokenDAO.saveToken(token);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return token;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		RequestDispatcher rd = null;
		// Env variable!
		// Twinfield accessToken and accessSecret
		String token = System.getenv("TWINFIELD_TOKEN");
		String secret = System.getenv("TWINFIELD_SECRET");
		String softwareName = req.getParameter("softwareName");
		Token checkToken = null;
		try {
			checkToken = getTempToken(token, secret, softwareToken, softwareName, false);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		req.getSession().setAttribute("error", null);
		// Check if accessToken exist in db
		if (checkToken != null) {
			Boolean newLogin = false;
			if (checkToken.getAccessToken() == null) {
				resp.sendRedirect(
						"https://login.twinfield.com/oauth/login.aspx?oauth_token=" + checkToken.getTempToken());
			} else if (checkToken.getAccessSecret().equals("invalid")) {
				rd = req.getRequestDispatcher("twinfield.jsp");
				req.getSession().setAttribute("session", null);
				req.getSession().setAttribute("error",
						"softwareToken is al in gebruik door " + checkToken.getSoftwareName());
				rd.forward(req, resp);
				// normal login
			} else {
				String sessionSession = (String) req.getSession().getAttribute("session");
				String sessionCluster = (String) req.getSession().getAttribute("cluster");
				String sessionID = null;
				String cluster = null;
				String[] array = null;
				
				array = SoapHandler.getSession(checkToken, sessionSession, sessionCluster);
				// Redirect user to login for the first time
				if (array == null) {
					newLogin = true;
					try {
						checkToken = getTempToken(token, secret, softwareToken, softwareName, newLogin);
						resp.sendRedirect("https://login.twinfield.com/oauth/login.aspx?oauth_token="
								+ checkToken.getTempToken());
					} catch (SQLException e) {
					}
				} else {
					sessionID = array[0];
					cluster = array[1];
				}
				
				logger.info("session= " + sessionID);
				logger.info("cluster= " + cluster);
				logger.info("WBAToken= " + softwareToken);
				if (sessionID != null) {
					// get all administrations
					ArrayList<String> offices = (ArrayList<String>) SoapHandler.createSOAPXML(sessionID, cluster,
							"<list><type>offices</type></list>", "office");
					// get all users
					ArrayList<Map<String, String>> users = new ArrayList<Map<String, String>>();
					Search searchObject = new Search("USR", "*", 0, 1, 100, null);
					ArrayList<String> responseArray = SoapHandler.createSOAPFinder(sessionID, cluster, searchObject);
					System.out.println("RESULTS LIST" + responseArray.toString());
					if (responseArray.isEmpty()) {
						System.out.println("TOKEN IS NIET MEER GELDIG");
						array = SoapHandler.getSession(checkToken, null, null);
						sessionID = array[0];
						cluster = array[1];
						System.out.println("TOKEN IS GELDIG sessionID" + sessionID);
						responseArray = SoapHandler.createSOAPFinder(sessionID, cluster, searchObject);
						// get all administrations
						offices = (ArrayList<String>) SoapHandler.createSOAPXML(sessionID, cluster,
								"<list><type>offices</type></list>", "office");
					}
					for (String s : responseArray) {
						Map<String, String> allUsers = new HashMap<String, String>();
						String[] split = s.split(",");
						allUsers.put("code", split[0]);
						allUsers.put("name", split[1]);
						users.add(allUsers);
					}
					
					rd = req.getRequestDispatcher("twinfield.jsp");
					req.getSession().setAttribute("session", sessionID);
					req.getSession().setAttribute("cluster", cluster);
					req.getSession().setAttribute("offices", offices);
					req.getSession().setAttribute("users", users);
					ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
					if (!allLogs.isEmpty() || allLogs != null) {
						req.getSession().setAttribute("logs", allLogs);
					}
					Settings set = ObjectDAO.getSettings(softwareToken);
					if (set != null) {
						Map<String, String> allImports = new HashMap<String, String>();
						for (String s : set.getImportObjects()) {
							allImports.put(s, "selected");
						}
						Map<String, String> allExports = new HashMap<String, String>();
						if (set.getExportObjects() != null) {
							for (String s : set.getExportObjects()) {
								allExports.put(s, "selected");
							}
						}
						req.getSession().setAttribute("exportCheckboxes", allExports);
						req.getSession().setAttribute("checkboxes", allImports);
						req.getSession().setAttribute("exportOffice", set.getExportOffice());
						req.getSession().setAttribute("factuur", set.getFactuurType());
						req.getSession().setAttribute("importOffice", set.getImportOffice());
						req.getSession().setAttribute("setUser", set.getUser());
						req.getSession().setAttribute("exportRelations", set.getExportObjects());
						System.out.println("exportRelations " + set.getExportObjects());
					}
					// if session is null
				} else {
					rd = req.getRequestDispatcher("twinfield.jsp");
					req.getSession().setAttribute("session", null);
					try {
						TokenDAO.deleteToken(softwareToken);
					} catch (SQLException e) {
						e.printStackTrace();
					}
					req.getSession().setAttribute("error", "Er ging iets mis tijdens het verbinden met Twinfield");
				}
				if (!newLogin) {
					rd.forward(req, resp);
				}
				
			}
		} else {
			rd = req.getRequestDispatcher("twinfield.jsp");
			req.getSession().setAttribute("session", null);
			req.getSession().setAttribute("error", "Er ging iets mis tijdens het verbinden met de Database");
			rd.forward(req, resp);
		}
	}
}