package controller.teamleader;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import controller.teamleader.util.BeanComparator;
import controller.teamleader.util.Dates;
import controller.teamleader.util.recursion.RecurseTarget;
import controller.teamleader.util.recursion.Recursor;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.workorder.*;
import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.istack.Nullable;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * The handler <tt>class</tt> for all Teamleader API calls.
 */
@SuppressWarnings("WeakerAccess")
public class TeamleaderHandler {
	
	private static final String SOFTWARE_NAME = "Teamleader";
	private static final String BASEURL = "https://app.teamleader.eu/api/";
	private static final Logger LOGGER = Logger.getLogger(SoapHandler.class.getName());
	private static final int MAX_RETRIES = 5;
	private static final int PAGE_SIZE = 100;
	
	// Simple caches for efficiency
	public final Map<String, TreeMap<String, String>> caches;
	// Our token
	private final Token token;
	// Our settings
	private final Settings settings;
	// The Date of the last update
	private final String date;
	
	private Boolean checkUpdate = false;
	
	// =======================================================================//
	
	public TeamleaderHandler(Token token, Settings settings, String date) {
		this.caches = new HashMap<>();
		this.token = token;
		this.settings = settings;
		this.date = date;
	}
	
	// =======================================================================//
	
	/**
	 * Process a <tt>request</tt> to Teamleader.
	 *
	 * <p>
	 * This will create a query from the provided parameters with the
	 * <tt>token</tt> credentials added, send the request to Teamleader and
	 * return an entitiy of the provided <tt>class</tt>.
	 *
	 * <p>
	 * The Teamleader API has a limit of 25 calls per 5 seconds per application,
	 * after which it will return an empty <tt>response</tt> with a
	 * <tt>status code</tt> of 429. If that's the case, we wait for 5 seconds
	 * before we try to perform the <tt>request</tt> again.
	 *
	 * <p>
	 * If we don't have a valid <tt>response</tt> after {@value #MAX_RETRIES}
	 * retries we throw an {@link HttpResponseException} with
	 * <tt>status code</tt> 400.
	 *
	 * @param <T>
	 *            The entitiy type. Must be either {@link JSONObject},
	 *            {@link JSONArray} or {@link String}.
	 *
	 * @param endpoint
	 *            The endpoint to connect to.
	 * @param token
	 *            A <tt>token</tt> containing credentials.
	 * @param params
	 *            A <tt>multivaluemap</tt> of query parameters.
	 * @param entityType
	 *            The <tt>class</tt> of the entitiy.
	 *
	 * @return An <tt>entity</tt> of type {@code <T>} containing the
	 *         <tt>response</tt> from the Teamleader API.
	 *
	 * @throws HttpResponseException
	 *             If the Teamleader API returns a status of "failed", or the
	 *             Teamleader API returns a malformed JSON message, or if we've
	 *             exhausted our {@link #MAX_RETRIES}.
	 *
	 *             <p>
	 *             In the case of a status of "failed", we include the detailed
	 *             reason provided by the Teamleader API.
	 */
	public static <T> T handleRequest(String endpoint, Token token, @Nullable MultivaluedMap<String, String> params,
			Class<T> entityType) throws HttpResponseException {
		
		if (!entityType.isAssignableFrom(JSONObject.class) && !entityType.isAssignableFrom(JSONArray.class)
				&& !entityType.isAssignableFrom(String.class)) {
			throw new IllegalArgumentException("entityType must be either JSONObject, JSONArray or String");
		}
		
		MultivaluedMap<String, String> data = params != null ? params : new MultivaluedHashMap<>();
		data.add("api_group", token.getAccessSecret());
		data.add("api_secret", token.getAccessToken());
		
		StringBuilder queryString = new StringBuilder();
		
		for (Map.Entry<String, List<String>> entry : data.entrySet()) {
			for (String value : entry.getValue()) {
				try {
					queryString = queryString.length() == 0 ? queryString : queryString.append('&');
					queryString = queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
							.append(URLEncoder.encode(value, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new Error(e); // Should never happen
				}
			}
		}
		
		// Start with the most generic client error.
		int lastCode = 400;
		String lastMessage = "Bad Request";
		
		RETRY: for (int i = 0; i < MAX_RETRIES; i++) {
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) new URL(BASEURL + endpoint).openConnection();
				connection.setUseCaches(true);
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(queryString.length()));
				
				BufferedWriter br = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
				br.write(queryString.toString());
				br.close();
				
				int inner = 5;
				do {
					int statusCode = connection.getResponseCode();
					String statusMessage = connection.getResponseMessage();
					Response.Status.Family statusFamily = Response.Status.Family.familyOf(statusCode);
					
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(
							statusCode / 100 == 2 ? connection.getInputStream() : connection.getErrorStream()))) {
						
						switch (statusFamily) {
						case SERVER_ERROR:
							throw new HttpResponseException(statusCode, statusMessage);
						case CLIENT_ERROR:
							if (connection.getResponseCode() == 429) { // API
																		// limit
																		// reached
								try {
									// Wait 5 seconds for API to accept our
									// requests again
									Thread.yield();
									Thread.sleep(5000);
								} catch (InterruptedException e) {
									/* Ignore */ }
								continue RETRY;
							} else {
								String reason;
								String body = reader.readLine();
								try {
									reason = new JSONObject(body).optString("reason");
								} catch (JSONException e) { // It wasn't JSON,
															// get the
															// underlying cause
									reason = body != null && !"".equals(body) ? body : statusMessage;
								}
								throw new HttpResponseException(statusCode, reason);
							}
						case SUCCESSFUL:
							try {
								return entityType.getConstructor(String.class).newInstance(reader.readLine());
							} catch (InvocationTargetException e) { // Malformed
																	// JSON
																	// message
								throw new HttpResponseException(statusCode, statusMessage);
							} catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
								throw new Error(e); // Should never happen
							}
						case INFORMATIONAL:
						case REDIRECTION:
							break;
						case OTHER:
							throw new HttpResponseException(statusCode, "Unrecognized HTTP status code in response");
						}
					}
					
					lastCode = statusCode;
					lastMessage = statusMessage;
				} while (inner-- > 0);
			} catch (HttpResponseException e1) {
				throw e1;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}
		}
		
		// Exhausted MAX_TRIES
		throw new HttpResponseException(lastCode,
				"No response from server after " + MAX_RETRIES + " tries. Reason: " + lastMessage);
	}
	
	// =======================================================================//
	
	/**
	 * @see #handleImport(ArrayList, String, String, Method, Method, Function)
	 */
	private <T> String[] handleImport(ArrayList<T> list, String type) throws IOException, SQLException {
		return handleImport(list, type, null);
	}
	
	/**
	 * @see #handleImport(ArrayList, String, String, Method, Method, Function)
	 */
	private <T> String[] handleImport(ArrayList<T> list, String type, @Nullable String addType)
			throws IOException, SQLException {
		return handleImport(list, type, addType, null);
	}
	
	/**
	 * @see #handleImport(ArrayList, String, String, Method, Method, Function)
	 */
	private <T> String[] handleImport(ArrayList<T> list, String type, @Nullable String addType,
			@Nullable Method consumer) throws IOException, SQLException {
		return handleImport(list, type, addType, consumer, null, null);
	}
	
	/**
	 * Process a <tt>list</tt> of entities to WorkOrderApp.
	 *
	 * <p>
	 * This will filter the <tt>list</tt> if the entities are already present on
	 * the database, sync the entities to the WorkOrderApp, create/update them
	 * to the database (depending on whether an entity already exists) and
	 * provide a message stating the import result.
	 *
	 * @param <T>
	 *            The entitiy type.
	 *
	 * @param list
	 *            A <tt>list</tt> of entities to process.
	 * @param type
	 *            The entitiy type, as a <tt>String</tt>.
	 * @param addType
	 *            The type that the entities will be synced as to WorkOrderApp.
	 * @param consumer
	 *            A (static!) <tt>method</tt> that will save the <tt>list</tt>
	 *            of entities to the database.
	 * @param producer
	 *            A (static!) <tt>method</tt> that will get the entitites in the
	 *            <tt>list</tt> of WorkorderApp for comparison.
	 * @param extractor
	 *            A <tt>function</tt> that will get the indexing key from the
	 *            entity so the <tt>producer</tt> can acquire the correct
	 *            entitites from the WorkorderApp.
	 * @return An array of 2 Strings:<br>
	 *         - A <tt>message</tt> stating the import result<br>
	 *         - Whether or not to save the current Date
	 *
	 * @throws IOException
	 *             If an error occurs while syncing data to WorkOrderApp.
	 * @throws SQLException
	 *             If an error occurs while saving data to the database.
	 */
	private <T> String[] handleImport(ArrayList<T> list, String type, @Nullable String addType,
			@Nullable Method consumer, @Nullable Method producer, @Nullable Function<T, Object> extractor)
			throws IOException, SQLException {
		
		StringBuilder message = new StringBuilder();
		
		if (!list.isEmpty()) {
			
			// Filter list of copies of elements that are already in
			// WorkorderApp.
			if (date != null && producer != null) {
				list.removeIf(element -> {
					try {
						Object dbElement = producer.invoke(null, token.getSoftwareToken(), extractor.apply(element));
						return BeanComparator.areFieldsEqual(element, dbElement);
					} catch (Exception e) {
						return false;
					}
				});
			}
			
			if (!list.isEmpty()) {
				
				// Add the remaining elements in the list to WorkOrderApp.
				Object result = WorkOrderHandler.addData(token.getSoftwareToken(), list,
						addType != null ? addType : type, SOFTWARE_NAME);
				
				if ((result instanceof JSONArray ? ((JSONArray) result).length() : (int) result) > 0) {
					
					// Save entities to database, if needed.
					if (consumer != null) {
						try {
							consumer.invoke(null, list, token.getSoftwareToken());
						} catch (ReflectiveOperationException e) {
							if (e.getCause() instanceof SQLException) {
								throw (SQLException) e.getCause();
							} else {
								throw new Error(e.getCause());
							}
						}
					}
					
					message = message.append(list.size()).append(' ')
							.append(list.size() == 1 ? type.substring(0, type.length() - 1) : type)
							.append(" imported<br>");
					checkUpdate = true;
				} else {
					message = message.append("Something went wrong with ").append(type).append("<br>");
				}
			} else {
				message = message.append("No ").append(type).append(" for import<br>");
			}
		} else {
			message = message.append("No ").append(type).append(" for import<br>");
		}
		
		return new String[] { message.toString(), Boolean.toString(checkUpdate) };
	}
	
	/**
	 * Process the result the export to the Teamleader API.
	 *
	 * @param logDetails
	 *            Export result details.
	 * @param amounts
	 *            An <tt>array</tt> with success and failure amounts.
	 *
	 * @return An array of 2 Strings:<br>
	 *         - A <tt>message</tt> stating the export result<br>
	 *         - The export result details.
	 */
	private String[] handleExport(StringBuilder logDetails, int[] amounts) {
		StringBuilder logMessage = new StringBuilder();
		if (amounts[1] > 0) {
			logMessage = logMessage.append(amounts[1]).append(amounts[1] == 1 ? " workorder " : " workorders ")
					.append("exported<br>");
		} else {
			logMessage = logMessage.append("No workorders exported<br>");
		}
		
		if (amounts[0] > 0) {
			logMessage = logMessage.append(amounts[0]).append(" out of ").append(amounts[0] + amounts[1])
					.append(" workorders(timetracking) have errors. Click for details<br>");
			settings.setFactuurType("error");
			try {
				ObjectDAO.saveSettings(settings, token.getSoftwareToken());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return new String[] { logMessage.toString(), logDetails.toString() };
	}
	
	// =======================================================================//
	
	/**
	 * Caches all neccessary items from the Teamleader API.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public void cache() throws HttpResponseException {
		cacheDepartments();
		cacheTaskTypes();
		cacheCustomFields();
	}
	
	/**
	 * Cache the Task Types from the Teamleader API.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public void cacheTaskTypes() throws HttpResponseException {
		JSONArray JSONArray = handleRequest("getTaskTypes.php", token, null, JSONArray.class);
		LOGGER.info("Task Types response " + JSONArray);
		
		caches.put("task_types", new TreeMap<>());
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("name");
				
				caches.get("task_types").put(Integer.toString(id), name);
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
	}
	
	/**
	 * Cache the Departments from the Teamleader API.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public void cacheDepartments() throws HttpResponseException {
		JSONArray JSONArray = handleRequest("getDepartments.php", token, null, JSONArray.class);
		LOGGER.info("Departments response " + JSONArray);
		
		caches.put("departments", new TreeMap<>());
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("name");
				
				caches.get("departments").put(Integer.toString(id), name);
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
	}
	
	/**
	 * Cache the Custom Fields from the Teamleader API.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public void cacheCustomFields() throws HttpResponseException {
		caches.put("custom_fields", new TreeMap<>());
		
		for (String s : new String[] { "todo", "project" }) {
			MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
			params.add("for", s);
			
			JSONArray JSONArray = handleRequest("getCustomFields.php", token, params, JSONArray.class);
			LOGGER.info("Custom Fields for " + s + " response " + JSONArray);
			
			for (int i = 0; i < JSONArray.length(); i++) {
				try {
					JSONObject object = JSONArray.getJSONObject(i);
					
					int id = object.getInt("id");
					String name = object.getString("name");
					
					if (!"wba_imported".equals(name))
						continue;
					
					caches.get("custom_fields").put(s, Integer.toString(id));
				} catch (JSONException e) {
					/* Malformed/incomplete JSON, ignore this entry */ }
			}
		}
		
		// Check if we have all the neccessary Custom Fields
		// 19-02-18: Removed dependency for Custom fields if the user doesn't
		// import the object it is mapped to.
		if (settings.getImportObjects().contains("assignments") && caches.get("custom_fields").get("todo") == null) {
			throw new RuntimeException("Custom Field \"todo\" missing");
		}
		if (settings.getImportObjects().contains("projects") && caches.get("custom_fields").get("project") == null) {
			throw new RuntimeException("Custom Field \"project\" missing");
		}
	}
	
	// =======================================================================//
	// Imports //
	// =======================================================================//
	
	/**
	 * Get the <tt>employees</tt> from the Teamleader API.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getEmployees() throws IOException, SQLException {
		JSONArray JSONArray = requestEmployees();
		
		ArrayList<Employee> employees = new ArrayList<>();
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("name");
				
				String[] nameArr = name.split("\\s+");
				String[] names = new String[] { "<leeg>", "<leeg>" };
				
				switch (nameArr.length) {
				case 0:
					break;
				case 1:
					names[0] = nameArr[0];
					break;
				case 2:
					names[0] = nameArr[0];
					names[1] = nameArr[1];
					break;
				default:
					names[0] = nameArr[0];
					StringBuilder lastNameBuilder = new StringBuilder();
					for (String name_ : nameArr) {
						lastNameBuilder = lastNameBuilder.append(name_).append(' ');
					}
					names[1] = lastNameBuilder.deleteCharAt(lastNameBuilder.length() - 1).toString();
					break;
				}
				
				employees.add(new Employee(names[0], names[1], Integer.toString(id)));
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
		
		return handleImport(employees, "users", "employees", forName("saveEmployees"), forName("getEmployee"),
				Employee::getCode);
		
	}
	
	/**
	 * Gets the name of an <tt>employee</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>employee</tt>.
	 *
	 * @return The name of the <tt>employee</tt>, or <code>null</code> if it
	 *         could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public String getEmployeeNameForID(int id) throws HttpResponseException {
		if (id < 1)
			return "";
		
		JSONArray JSONArray = requestEmployees();
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				if (object.getInt("id") == id) {
					return object.getString("name");
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
		
		return "";
	}
	
	/**
	 * Request <tt>employees</tt> from the Teamleader API.
	 *
	 * @return A <tt>JSONArray</tt> with <tt>employees</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestEmployees() throws HttpResponseException {
		JSONArray JSONArray = handleRequest("getUsers.php", token, null, JSONArray.class);
		LOGGER.info("Employees response " + JSONArray);
		return JSONArray;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>materials</tt> from the Teamleader API.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getMaterials() throws IOException, SQLException {
		ArrayList<Material> materials = Recursor.recurseImpl(this::getMaterials_);
		return handleImport(materials, "products", "materials", forName("saveMaterials"));
	}
	
	/**
	 * Get a part of the <tt>materials</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @param args
	 *            Not used in this method. Required for {@link RecurseTarget}.
	 *
	 * @return A <tt>list</tt> containing <tt>materials</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<Material> getMaterials_(int pageNo, @SuppressWarnings("unused") Object... args)
			throws HttpResponseException {
		JSONArray JSONArray = requestMaterials(pageNo);
		
		ArrayList<Material> materials = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				int id = JSONArray.getJSONObject(i).getInt("id");
				
				JSONObject object = requestMaterial(id);
				
				String idExt = object.optString("external_id");
				String name = object.getString("name");
				long modified = object.getLong("date_edited");
				
				double price = 0.0D;
				
				JSONArray prices = object.optJSONArray("prices");
				if (prices != null) {
					price = Double.parseDouble(prices.getJSONObject(0).getString("price_excl_vat"));
				}
				
				if (!materials.add(new Material(Integer.toString(id), idExt, "", name, price, null,
						Dates.toDate(modified, Dates.DATE), Integer.toString(id)))) {
					failures++;
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (materials.size() + failures == PAGE_SIZE) {
			materials.add(null);
		}
		
		return materials;
	}
	
	/**
	 * Request 100 <tt>materials</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @return A <tt>JSONArray</tt> with <tt>materials</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestMaterials(int pageNo) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("amount", Integer.toString(PAGE_SIZE));
		params.add("pageno", Integer.toString(pageNo));
		
		if (date != null) {
			try {
				if (ObjectDAO.hasContent(token.getSoftwareToken(), "materials")) {
					params.add("modifiedsince", Long.toString(Dates.toTimestamp(date, Dates.DATE_TIME)));
				}
			} catch (SQLException e) {
				/* Ignore */ }
		}
		
		JSONArray JSONArray = handleRequest("getProducts.php", token, params, JSONArray.class);
		LOGGER.info("Materials response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * Request a <tt>material</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>material</tt>.
	 *
	 * @return A <tt>JSONObject</tt> with the <tt>material</tt>, or
	 *         <code>null</code> if it could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONObject requestMaterial(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("product_id", Integer.toString(id));
		
		JSONObject object = handleRequest("getProduct.php", token, params, JSONObject.class);
		LOGGER.info("Product " + id + " response " + object);
		return object;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>hour types</tt> from the Teamleader API.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getHourtypes() throws IOException, SQLException {
		JSONArray JSONArray = requestHourtypes();
		
		ArrayList<HourType> hourtypes = new ArrayList<>();
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.optString("name");
				
				if (!"".equals(name)) {
					hourtypes.add(new HourType(Integer.toString(id), name, 0, 0, 0, 0, 1, null, Integer.toString(id)));
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
		
		return handleImport(hourtypes, "tasktypes", "hourtypes", forName("saveHourTypes"), forName("getHourType"),
				HourType::getCode);
	}
	
	/**
	 * Request <tt>hour types</tt> from the Teamleader API.
	 *
	 * @return A <tt>JSONArray</tt> containing <tt>hour types</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestHourtypes() throws HttpResponseException {
		JSONArray JSONArray = handleRequest("getTaskTypes.php", token, null, JSONArray.class);
		LOGGER.info("Hourtypes response " + JSONArray);
		return JSONArray;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>relations</tt> from the Teamleader API. This includes
	 * companies and contacts.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getRelations() throws IOException, SQLException {
		ArrayList<Relation> relations = Recursor.recurseImpl(this::getRelations_);
		
		String[] result = handleImport(relations, "companies", "relations", forName("saveRelations"));
		
		if (Boolean.parseBoolean(result[1])) {
			handleImport(relations, "contactpersons");
			handleImport(relations, "addresses");
		}
		
		ArrayList<Relation> contacts = new ArrayList<>(Recursor.recurseImpl(this::getContacts_));
		String[] rslt = handleImport(contacts, "contacts", "relations", forName("saveRelations"));
		result[0] += rslt[0];
		result[1] += rslt[1];
		
		return result;
	}
	
	/**
	 * Get a part of the <tt>relations</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @param args
	 *            Not used in this method. Required for {@link RecurseTarget}.
	 *
	 * @return A <tt>list</tt> of <tt>relations</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<Relation> getRelations_(int pageNo, @SuppressWarnings("unused") Object... args)
			throws HttpResponseException {
		JSONArray JSONArray = requestRelations(pageNo);
		
		ArrayList<Relation> relations = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("name");
				String email = object.getString("email");
				long modified = object.getLong("date_edited");
				
				String telephone = object.getString("telephone");
				String street = object.getString("street");
				String number = object.getString("number");
				String zipcode = object.getString("zipcode");
				String city = object.getString("city");
				
				ArrayList<Address> addresses = new ArrayList<>(this.getAddressesFor(id));
				
				String contactName = name;
				if (addresses.size() > 0) {
					contactName = addresses.get(0).getName();
				}
				addresses.add(
						new Address(contactName, telephone, email, street, number, zipcode, city, null, "main", id));
				
				if (!relations.add(new Relation(name, Integer.toString(id), contactName, email, addresses,
						Dates.toDate(modified, Dates.DATE), Integer.toString(id)))) {
					failures++;
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (relations.size() + failures == PAGE_SIZE) {
			relations.add(null);
		}
		return relations;
	}
	
	/**
	 * Get a <tt>relation</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>relation</tt>.
	 *
	 * @return A <tt>relation</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws JSONException
	 *             If the JSON <tt>response</tt> was malformed or incomplete.
	 */
	public Relation getRelationForID(int id) throws HttpResponseException, JSONException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		JSONObject object = requestRelation(id);
		
		String name = object.getString("name");
		String email = object.getString("email");
		long modified = object.getLong("date_edited");
		
		String telephone = object.getString("telephone");
		String street = object.getString("street");
		String number = object.getString("number");
		String zipcode = object.getString("zipcode");
		String city = object.getString("city");
		
		ArrayList<Address> addresses = new ArrayList<>();
		addresses.add(new Address(name, telephone, email, street, number, zipcode, city, null, "main", id));
		addresses.addAll(this.getAddressesFor(id));
		
		String contact = addresses.size() > 1 ? addresses.get(1).getName() : "";
		
		return new Relation(name, Integer.toString(id), contact, email, addresses, Dates.toDate(modified, Dates.DATE),
				Integer.toString(id));
	}
	
	/**
	 * Get a part of the <tt>contacts</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @param args
	 *            Not used in this method. Required for {@link RecurseTarget}.
	 *
	 * @return A <tt>list</tt> of <tt>contacts</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<Relation> getContacts_(int pageNo, @SuppressWarnings("unused") Object... args)
			throws HttpResponseException {
		JSONArray JSONArray = requestContacts(pageNo);
		
		ArrayList<Relation> contacts = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("forename") + " " + object.getString("surname");
				long modified = object.getLong("date_edited");
				
				String telephone = object.getString("telephone");
				String gsm = object.getString("gsm");
				String email = object.getString("email");
				String street = object.getString("street");
				String number = object.getString("number");
				String zipcode = object.getString("zipcode");
				String city = object.getString("city");
				
				if ("".equals(telephone)) {
					telephone = gsm;
				}
				
				ArrayList<Address> addresses = new ArrayList<>();
				
				if (!addresses
						.add(new Address("", telephone, email, street, number, zipcode, city, null, "main", id))) {
					failures++;
				}
				
				if (!contacts.add(new Relation(name, Integer.toString(id), "", email, addresses,
						Dates.toDate(modified, Dates.DATE), Double.toString(id)))) {
					failures++;
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (contacts.size() + failures == PAGE_SIZE) {
			contacts.add(null);
		}
		return contacts;
	}
	
	/**
	 * Get a <tt>contact</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>contact</tt>.
	 *
	 * @return A <tt>contact</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws JSONException
	 *             If the JSON <tt>response</tt> was malformed or incomplete.
	 */
	public Relation getContactForID(int id) throws HttpResponseException, JSONException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		JSONObject object = requestContact(id);
		
		String name = object.getString("forename") + " " + object.getString("surname");
		long modified = object.getLong("date_edited");
		
		String telephone = object.getString("telephone");
		String gsm = object.getString("gsm");
		String email = object.getString("email");
		String street = object.getString("street");
		String number = object.getString("number");
		String zipcode = object.getString("zipcode");
		String city = object.getString("city");
		
		if ("".equals(telephone)) {
			telephone = gsm;
		}
		
		ArrayList<Address> addresses = new ArrayList<>();
		addresses.add(new Address(name, telephone, email, street, number, zipcode, city, null, "contact", id));
		
		return new Relation(name, Integer.toString(id), name, email, addresses, Dates.toDate(modified, Dates.DATE),
				Double.toString(id));
	}
	
	/**
	 * Request 100<tt>relations</tt> from the Teamleader API.
	 * 
	 * @param pageNo
	 *            The current iteration.
	 * @return A <tt>JSONArray</tt> with <tt>relations</tt>.
	 * @throws HttpResponseException
	 *             See {@link #handleRequest}for details.
	 * 
	 */
	
	public JSONArray requestRelations(int pageNo) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("amount", Integer.toString(PAGE_SIZE));
		params.add("pageno", Integer.toString(pageNo));
		if (date != null) {
			try {
				if (ObjectDAO.hasContent(token.getSoftwareToken(), "relations")) {
					params.add("modifiedsince", Long.toString(Dates.toTimestamp(date, Dates.DATE_TIME)));
				}
			} catch (SQLException e) {
				/* Ignore */
			}
		}
		JSONArray JSONArray = handleRequest("getCompanies.php", token, params, JSONArray.class);
		LOGGER.info("Relations response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * 
	 * Request a <tt>relation</tt> from the Teamleader API.
	 * 
	 * @param id
	 *            The id of the <tt>relation</tt>.
	 * @return A <tt>JSONObject</tt> with the <tt>relation</tt>, or
	 *         <code>null</code> if it could not be found.
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * 
	 */
	
	public JSONObject requestRelation(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("company_id", Integer.toString(id));
		JSONObject object = handleRequest("getCompany.php", token, params, JSONObject.class);
		LOGGER.info("Relation " + id + " response " + object);
		return object;
	}
	
	/**
	 * 
	 * 
	 * /** Request 100 <tt>contacts</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 *
	 * @return A <tt>JSONArray</tt> with <tt>contacts</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	private JSONArray requestContacts(int pageNo) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("amount", Integer.toString(PAGE_SIZE));
		params.add("pageno", Integer.toString(pageNo));
		
		if (date != null) {
			params.add("modifiedsince", Long.toString(Dates.toTimestamp(date, Dates.DATE_TIME)));
		}
		
		JSONArray JSONArray = handleRequest("getContacts.php", token, params, JSONArray.class);
		LOGGER.info("Contacts response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * Request a <tt>contact</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>contact</tt>.
	 *
	 * @return A <tt>JSONObject</tt> with the <tt>contact</tt>, or
	 *         <code>null</code> if it could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONObject requestContact(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("contact_id", Integer.toString(id));
		
		JSONObject object = handleRequest("getContact.php", token, params, JSONObject.class);
		LOGGER.info("Relation " + id + " response " + object);
		return object;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>addresses</tt> from the Teamleader API.
	 *
	 * @return A <tt>list</tt> of <tt>addresses</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public ArrayList<Address> getAddressesFor(int company_id) throws HttpResponseException {
		return Recursor.recurseImpl(this::getAddressesFor_, company_id);
	}
	
	/**
	 * Get a part of the <tt>addresses</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration. Not used in this method. Required for
	 *            {@link RecurseTarget}.
	 * @param args
	 *            A <tt>vararg parameter</tt> containing a <tt>String</tt> with
	 *            the company name you're trying to find the <tt>addresses</tt>
	 *            for.
	 *
	 * @return A <tt>list</tt> of <tt>addresses</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<Address> getAddressesFor_(@SuppressWarnings("unused") int pageNo, Object... args)
			throws HttpResponseException {
		if (args.length != 1 || !(args[0] instanceof Integer))
			throw new IllegalArgumentException("getAddressesFor_ args requires exactly one argument of type int");
		
		JSONArray JSONArray = requestAddressesFor((int) args[0]);
		
		ArrayList<Address> addresses = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				int id = object.getInt("id");
				String name = object.getString("name");
				String telephone = object.getString("telephone");
				String gsm = object.getString("gsm");
				String email = object.getString("email");
				String street = object.getString("street");
				String number = object.getString("number");
				String zipcode = object.getString("zipcode");
				String city = object.getString("city");
				
				if ("".equals(telephone)) {
					telephone = gsm;
				}
				
				if (!addresses
						.add(new Address(name, telephone, email, street, number, zipcode, city, null, "contact", id))) {
					failures++;
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (addresses.size() + failures == PAGE_SIZE) {
			addresses.add(null);
		}
		return addresses;
	}
	
	/**
	 * Request all <tt>addresses</tt> related to a <tt>relation</tt> from the
	 * Teamleader API.
	 *
	 * @param company_id
	 *            The id of the <tt>relation</tt> you're trying to find the
	 *            <tt>addresses</tt> for.
	 *
	 * @return A <tt>JSONArray</tt> with <tt>addresses</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestAddressesFor(int company_id) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("company_id", Integer.toString(company_id));
		
		JSONArray JSONArray = handleRequest("getContactsByCompany.php", token, params, JSONArray.class);
		LOGGER.info("Addresses response " + JSONArray);
		return JSONArray;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>projects</tt> from the Teamleader API.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getProjects() throws IOException, SQLException {
		ArrayList<Project> projects = Recursor.recurseImpl(this::getProjects_);
		return handleImport(projects, "projects", null, forName("saveProjects"));
	}
	
	/**
	 * Get a part of the <tt>projects</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @param args
	 *            Not used in this method. Required for {@link RecurseTarget}.
	 *
	 * @return A <tt>list</tt> of <tt>projects</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<Project> getProjects_(int pageNo, @SuppressWarnings("unused") Object... args)
			throws HttpResponseException {
		JSONArray JSONArray = requestProjects(pageNo);
		
		ArrayList<Project> projects = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				String i_id = caches.get("custom_fields").get("project");
				
				if (i_id != null && !"".equals(i_id)) {
					// Already imported
					if ("1".equals(object.optString("cf_value_" + i_id)))
						continue;
				}
				
				int id = object.getInt("id");
				String title = object.getString("title");
				
				if (projects.addAll(getProjectsForID(id))) {
					MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
					params.add("project_id", Integer.toString(id));
					params.add("title", title);
					params.add("track_changes", "0");
					params.add("custom_field_" + caches.get("custom_fields").get("project"), "1");
					handleRequest("updateProject.php", token, params, String.class);
				} else {
					failures++;
				}
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (projects.size() + failures == PAGE_SIZE) {
			projects.add(null);
		}
		return projects;
	}
	
	/**
	 * Get a <tt>project</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>project</tt>.
	 *
	 * @return A <tt>list</tt> of <tt>projects</tt>, containing all
	 *         <tt>sub-projects</tt> of this <tt>project</tt>
	 *         (<tt>milestones</tt>).
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public ArrayList<Project> getProjectsForID(int id) throws HttpResponseException {
		JSONObject jsonProject = requestProject(id);
		
		ArrayList<Project> projects = new ArrayList<>();
		try {
			
			int coc_id = jsonProject.getInt("contact_or_company_id");
			String phase = jsonProject.getString("phase");
			String title = jsonProject.getString("title");
			long start_date = jsonProject.getLong("start_date");
			String description = jsonProject.getString("description_html");
			
			JSONArray milestones = this.requestProjectMilestones(id);
			
			for (int i = 0; i < milestones.length(); i++) {
				JSONObject jsonMilestone = milestones.getJSONObject(i);
				
				int id_ = jsonMilestone.getInt("id");
				long due_date = jsonMilestone.getLong("due_date");
				String title_ = jsonMilestone.getString("title");
				int closed = jsonMilestone.getInt("closed");
				
				projects.add(new Project(Integer.toString(id) + '#' + Integer.toString(id_), Integer.toString(id_),
						Integer.toString(coc_id), phase, title + " - " + title_,
						Dates.toDate(start_date, Dates.DATE_INV), Dates.toDate(due_date, Dates.DATE_INV), description,
						closed == 0 ? 0 : 100, closed == 0 ? 1 : 0, null));
				
				start_date = due_date;
			}
		} catch (JSONException e) {
			/* Malformed/incomplete JSON, ignore this entry */ }
			
		return projects;
	}
	
	/**
	 * Request 100 <tt>projects</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 *
	 * @return A <tt>JSONArray</tt> with <tt>projects</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestProjects(int pageNo) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("amount", Integer.toString(PAGE_SIZE));
		params.add("pageno", Integer.toString(pageNo));
		params.add("show_active_only", "1");
		
		String i_id = caches.get("custom_fields").get("project");
		
		if (i_id != null && !"".equals(i_id)) {
			params.add("selected_customfields", i_id);
		}
		
		JSONArray JSONArray = handleRequest("getProjects.php", token, params, JSONArray.class);
		LOGGER.info("Projects response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * Request a <tt>project</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>project</tt>.
	 *
	 * @return A <tt>JSONObject</tt> with the <tt>project</tt>, or
	 *         <code>null</code> if it could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONObject requestProject(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("project_id", Integer.toString(id));
		
		JSONObject object = handleRequest("getProject.php", token, params, JSONObject.class);
		LOGGER.info("Project " + id + " response " + object);
		return object;
	}
	
	/**
	 * Request all <tt>milestones</tt> related to a <tt>project</tt> from the
	 * Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>projects</tt>.
	 *
	 * @return A <tt>JSONArray</tt> with the <tt>milestones</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestProjectMilestones(int id) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("project_id", Integer.toString(id));
		
		JSONArray JSONArray = handleRequest("getMilestonesByProject.php", token, params, JSONArray.class);
		LOGGER.info("Milestones response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * Request a <tt>milestone</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>milestone</tt>.
	 *
	 * @return A <tt>JSONObject</tt> with the <tt>milestone</tt>, or
	 *         <code>null</code> if it could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONObject requestProjectMilestone(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("milestone_id", Integer.toString(id));
		
		JSONObject object = handleRequest("getMilestone.php", token, params, JSONObject.class);
		LOGGER.info("Milestone " + id + " response " + object);
		return object;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>work orders</tt> from the Teamleader API.
	 *
	 * @return The result of {@link #handleImport}.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws IOException
	 *             See {@link #handleImport} for details.
	 * @throws SQLException
	 *             See {@link #handleImport} for details.
	 */
	public String[] getWorkOrders() throws IOException, SQLException {
		ArrayList<WorkOrder> workOrders = Recursor.recurseImpl(this::getWorkOrders_);
		return handleImport(workOrders, "tasks", "PostWorkorders");
	}
	
	/**
	 * Get a part of the <tt>work orders</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 * @param args
	 *            Not used in this method. Required for {@link RecurseTarget}.
	 *
	 * @return A <tt>list</tt> of <tt>assignments</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	@RecurseTarget
	public ArrayList<WorkOrder> getWorkOrders_(int pageNo, @SuppressWarnings("unused") Object... args)
			throws HttpResponseException {
		JSONArray JSONArray = requestWorkOrders(pageNo);
		
		ArrayList<WorkOrder> workOrders = new ArrayList<>();
		int failures = 0;
		
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				String i_id = caches.get("custom_fields").get("todo");
				
				if (i_id != null && !"".equals(i_id)) {
					// Already imported
					if ("1".equals(object.optString("cf_value_" + i_id)))
						continue;
				}
				
				int id = object.getInt("id");
				int p_id = object.optInt("project_id");
				int m_id = object.optInt("milestone_id");
				
				if (workOrders.add(this.getWorkOrderForID(id, p_id, m_id))) {
					MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
					params.add("task_id", Integer.toString(id));
					params.add("custom_field_" + caches.get("custom_fields").get("todo"), "1");
					handleRequest("updateTask.php", token, params, String.class);
				} else {
					failures++;
				}
			} catch (JSONException e1) {
				/* Malformed/incomplete JSON, ignore this entry */
				failures++;
			}
		}
		
		if (workOrders.size() + failures == PAGE_SIZE) {
			workOrders.add(null);
		}
		return workOrders;
	}
	
	/**
	 * Get a <tt>work order</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>work order</tt>.
	 * @param project_id
	 *            The id of the <tt>project</tt>.
	 * @param milestone_id
	 *            The id of the <tt>milestone</tt>.
	 *
	 * @return A <tt>work order</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 * @throws JSONException
	 *             If the JSON <tt>response</tt> was malformed or incomplete.
	 */
	public WorkOrder getWorkOrderForID(int id, int project_id, int milestone_id)
			throws HttpResponseException, JSONException {
		JSONObject object = requestWorkOrder(id);
		
		String description = object.getString("description");
		long due_date = object.getLong("due_date");
		String client_type = object.getString("client_type");
		int client_id = object.getInt("client_id");
		int type_id = object.getInt("type_id");
		
		String projectNr = project_id != 0 && milestone_id != 0
				? Integer.toString(project_id) + "#" + Integer.toString(milestone_id) : "";
		String projectNrExt = milestone_id != 0 ? Integer.toString(milestone_id) : "";
		String phase;
		
		if (project_id > 0) {
			JSONObject project = requestProject(project_id);
			phase = project.getString("phase");
		} else {
			phase = "active";
		}
		
		String billing_type;
		int resp_user_id;
		
		if (milestone_id > 0) {
			JSONObject milestone = requestProjectMilestone(milestone_id);
			billing_type = milestone.getString("billing_type");
			resp_user_id = milestone.getInt("responsible_user_id");
		} else {
			billing_type = "";
			resp_user_id = 0;
		}
		
		// 01-03-18: Added support for Tasks linked to Contacts
		Relation relation = "company".equals(client_type) ? getRelationForID(client_id) : getContactForID(client_id);
		
		String email = relation.getAddressess().get(0).getEmail();
		String invoice_email = relation.getEmailWorkorder();
		
		String task_type = caches.get("task_types").get(Integer.toString(type_id));
		
		ArrayList<WorkPeriod> workPeriods = this.getWorkPeriodsForID(id, project_id, task_type, description);
		ArrayList<Relation> relations = new ArrayList<>();
		relations.add(relation);
		
		return new WorkOrder(projectNr, Dates.toDate(due_date, Dates.DATE), invoice_email, email,
				Integer.toString(client_id), phase, billing_type, null, Dates.getCurrentDate(Dates.DATE),
				Integer.toString(id), Integer.toString(id), workPeriods, relations, null,
				Dates.toDate(due_date, Dates.DATE), null, projectNrExt, task_type, description, Dates.getCurrentDate(),
				null, phase, resp_user_id != 0 ? Integer.toString(resp_user_id) : "");
	}
	
	/**
	 * Request 100 <tt>work orders</tt> from the Teamleader API.
	 *
	 * @param pageNo
	 *            The current iteration.
	 *
	 * @return A <tt>JSONArray</tt> with <tt>work orders</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestWorkOrders(int pageNo) throws HttpResponseException {
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("amount", Integer.toString(PAGE_SIZE));
		params.add("pageno", Integer.toString(pageNo));
		
		String i_id = caches.get("custom_fields").get("todo");
		
		if (i_id != null && !"".equals(i_id)) {
			params.add("selected_customfields", i_id);
		}
		
		JSONArray JSONArray = handleRequest("getTasks.php", token, params, JSONArray.class);
		LOGGER.info("Assignments response " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * Request a <tt>work order</tt> from the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>work order</tt>.
	 *
	 * @return A <tt>JSONObject</tt> with the <tt>work order</tt>, or
	 *         <code>null</code> if it could not be found.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONObject requestWorkOrder(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("task_id", Integer.toString(id));
		
		JSONObject object = handleRequest("getTask.php", token, params, JSONObject.class);
		LOGGER.info("Assignment " + id + " response " + object);
		return object;
	}
	
	// =======================================================================//
	
	/**
	 * Get the <tt>work periods</tt> from the Teamleader API.
	 *
	 * @return A <tt>list</tt> of <tt>work periods</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public ArrayList<WorkPeriod> getWorkPeriodsForID(int id, int project_id, String task_type, String description)
			throws HttpResponseException {
		JSONArray JSONArray = requestWorkPeriods(id);
		
		ArrayList<WorkPeriod> workPeriods = new ArrayList<>();
		for (int i = 0; i < JSONArray.length(); i++) {
			try {
				JSONObject object = JSONArray.getJSONObject(i);
				
				// Ignore entries that are not invoiceable
				if (object.getInt("is_invoiceable") != 1)
					continue;
				
				int id_ = object.getInt("id");
				int user_id = object.getInt("user_id");
				long date = object.getLong("date");
				String duration = object.getString("duration");
				
				workPeriods.add(new WorkPeriod(Integer.toString(user_id), task_type, Dates.toDate(date, Dates.DATE),
						Integer.toString(project_id), description, duration, Integer.toString(id_), null, null));
			} catch (JSONException e) {
				/* Malformed/incomplete JSON, ignore this entry */ }
		}
		
		return workPeriods;
	}
	
	/**
	 * Request all <tt>work periods</tt> related to a <tt>work order</tt> from
	 * the Teamleader API.
	 *
	 * @param id
	 *            The id of the <tt>work order</tt>.
	 *
	 * @return A <tt>JSONArray</tt> with the <tt>work periods</tt>.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public JSONArray requestWorkPeriods(int id) throws HttpResponseException {
		if (id < 1)
			throw new IllegalArgumentException("id cannot be less than 1");
		
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		params.add("task_id", Integer.toString(id));
		
		JSONArray JSONArray = handleRequest("getTimetrackingForTask.php", token, params, JSONArray.class);
		LOGGER.info("Workperiods response " + JSONArray);
		return JSONArray;
	}
	
	// =======================================================================//
	
	/**
	 * Set the <tt>work orders</tt> to the Teamleader API. *
	 * 
	 * @return See {@link #handleExport} for details.
	 *
	 * @throws HttpResponseException
	 *             See {@link #handleRequest} for details.
	 */
	public String[] setWorkOrders() throws HttpResponseException {
		List<WorkOrder> workOrders = WorkOrderHandler.getData(token.getSoftwareToken(), "GetWorkorders",
				settings.getFactuurType(), false, SOFTWARE_NAME);
		
		StringBuilder logDetails = new StringBuilder();
		int[] amounts = { 0, 0 };
		
		try {
			for (WorkOrder workOrder : workOrders) {
				if (!"Compleet".equals(workOrder.getStatus()))
					continue;
				
				try {
					if (workOrder.getWorkPeriods().size() == 0) {
						throw new NoSuchElementException(
								"No workperiods found on workorder " + workOrder.getWorkorderNr() + "\n");
					}
					
					// if ("".equals(workOrder.getProjectNr()) ||
					// "0".equals(workOrder.getProjectNr())) {
					// // Factuur
					// handleRequest("addInvoice.php", postInvoice(settings,
					// workOrder), String.class);
					// } else {
					
					// timetracking
					boolean first = true;
					for (WorkPeriod workPeriod : workOrder.getWorkPeriods()) {
						handleRequest("addTimetracking.php", token, postTimetracking(workOrder, workPeriod, first),
								String.class);
						first = false;
					}
					
					// }
					amounts[1]++;
					WorkOrderHandler.setWorkorderStatus(workOrder.getId(), workOrder.getWorkorderNr(), true,
							"GetWorkorder", token.getSoftwareToken(), SOFTWARE_NAME);
				} catch (NoSuchElementException e) {
					logDetails = logDetails.append(e.getMessage());
					amounts[0]++;
				}
			}
		} catch (SQLException e) {
			throw new HttpResponseException(400, e.getMessage());
		}
		
		return handleExport(logDetails, amounts);
	}
	
	/**
	 * Construct a {@link MultivaluedMap} containing POST parameters for an
	 * <tt>invoice</tt>.
	 *
	 * @param workOrder
	 *            The <tt>work order</tt>.
	 *
	 * @return A {@link MultivaluedMap} containing POST parameters.
	 *
	 * @throws SQLException
	 *             If an error occures while getting data from the database.
	 */
	@Deprecated
	public MultivaluedMap<String, String> postInvoice(WorkOrder workOrder) throws SQLException {
		
		MultivaluedMap<String, String> invoice = new MultivaluedHashMap<>();
		
		String client_id = workOrder.getCustomerDebtorNr();
		String department = caches.get("departments").keySet().iterator().next();
		// TODO: Add an option to Settings so the user can pick the department
		// instead of always picking the first.
		
		if (workOrder.getRelations() == null || workOrder.getRelations().size() < 1) {
			throw new NoSuchElementException(
					"Relations on workorder " + workOrder.getWorkorderNr() + " do not exist\n");
		}
		
		Relation relation = workOrder.getRelations().get(0);
		
		if (relation == null) {
			throw new NoSuchElementException("Relation on workorder " + workOrder.getWorkorderNr() + " do not exist\n");
		}
		
		// 01-03-18: Added support for Tasks linked to Contacts
		String for_ = "contact".equals(relation.getAddressess().get(0).getType()) ? "contact" : "company";
		
		invoice.add("contact_or_company", for_);
		invoice.add("contact_or_company_id", client_id);
		invoice.add("sys_department_id", department);
		
		if ("draft".equals(settings.getFactuurType())) {
			invoice.add("draft_invoice", "1");
		}
		
		int q = 0;
		for (WorkPeriod workPeriod : workOrder.getWorkPeriods()) {
			q++;
			
			HourType hourtype = WorkOrderHandler.getHourTypes(token.getSoftwareToken(), workPeriod.getHourType(),
					SOFTWARE_NAME);
			if (hourtype == null) {
				throw new NoSuchElementException(
						"Hourtype on workorder " + workOrder.getWorkorderNr() + " does not exist in WorkOrderApp\n");
			}
			
			String description = workPeriod.getDescription();
			long duration = Dates.roundM(workPeriod.getDuration(), settings.getRoundedHours());
			
			invoice.add("description_" + q, description);
			invoice.add("price_" + q, Double.toString(50D)); // TODO:
																// Teamleader,
																// why can't I
																// get the Task
																// Type price???
			invoice.add("amount_" + q, Double.toString(duration / 60));
			invoice.add("vat_" + q, "CM");
		}
		
		for (Material material : workOrder.getMaterials()) {
			q++;
			
			String code = material.getCode();
			
			if (ObjectDAO.getMaterial(token.getSoftwareToken(), code) == null) {
				throw new NoSuchElementException(
						"Material on workorder " + workOrder.getWorkorderNr() + " does not exist in WorkOrderApp\n");
			}
			
			String description = material.getDescription();
			double price = material.getPrice();
			String quantity = material.getQuantity();
			
			invoice.add("description_" + q, description);
			invoice.add("price_" + q, Double.toString(price));
			invoice.add("amount_" + q, quantity);
			invoice.add("vat_" + q, "21");
			invoice.add("product_id_" + q, code);
		}
		
		return invoice;
	}
	
	/**
	 * Construct a {@link MultivaluedMap} containing POST parameters for a
	 * <tt>timetracking</tt>.
	 *
	 * @param workOrder
	 *            The <tt>work order</tt>.
	 * @param workPeriod
	 *            The <tt>work period</tt>.
	 * @param first
	 *            If this <tt>timetracking</tt> is the first of the
	 *            <tt>work periods</tt> on this <tt>work order</tt>.
	 *
	 * @return A {@link MultivaluedMap} containing POST parameters.
	 *
	 * @throws SQLException
	 *             If an error occures while getting data from the database.
	 */
	public MultivaluedMap<String, String> postTimetracking(WorkOrder workOrder, WorkPeriod workPeriod, boolean first)
			throws SQLException {
		MultivaluedMap<String, String> timetracking = new MultivaluedHashMap<>();
		
		HourType hourtype = WorkOrderHandler.getHourTypes(token.getSoftwareToken(), workPeriod.getHourType(),
				SOFTWARE_NAME);
		if (hourtype == null) {
			throw new NoSuchElementException(
					"Hourtype on workorder " + workOrder.getWorkorderNr() + " does not exist in WorkOrderApp\n");
		}
		
		if (workOrder.getRelations() == null || workOrder.getRelations().size() < 1) {
			throw new NoSuchElementException(
					"Relations on workorder " + workOrder.getWorkorderNr() + " do not exist\n");
		}
		
		Relation relation = workOrder.getRelations().get(0);
		
		if (relation == null) {
			throw new NoSuchElementException("Relation on workorder " + workOrder.getWorkorderNr() + " do not exist\n");
		}
		
		String description = workPeriod.getDescription();
		String date = workPeriod.getWorkDate();
		String start_time = workPeriod.getBeginTime();
		String end_time = workPeriod.getEndTime();
		String worker_id = workPeriod.getEmployeeNr();
		
		String for_;
		String for_id = workOrder.getExternProjectNr();
		String task_id = workOrder.getWorkorderNr();
		
		if ("".equals(description)) {
			description = workOrder.getWorkDescription();
		}
		
		if ("".equals(for_id)) {
			// 01-03-18: Added support for Tasks linked to Contacts
			for_ = "contact".equals(relation.getAddressess().get(0).getType()) ? "contact" : "company";
			for_id = workOrder.getCustomerDebtorNr();
		} else {
			for_ = "project_milestone";
		}
		
		// 27-02-18: Fixed rounding.
		long start_date = Dates.roundS(Dates.toTimestamp(date + " " + start_time), settings.getRoundedHours());
		long end_date = Dates.roundS(Dates.toTimestamp(date + " " + end_time), settings.getRoundedHours());
		
		timetracking.add("description", description);
		timetracking.add("start_date", Long.toString(start_date));
		timetracking.add("end_date", Long.toString(end_date));
		timetracking.add("worker_id", worker_id);
		timetracking.add("task_type_id", hourtype.getCode());
		
		timetracking.add("for", for_);
		timetracking.add("for_id", for_id);
		timetracking.add("related_object_type", "task");
		timetracking.add("related_object_id", task_id);
		timetracking.add("invoiceable", "1");
		
		if (first) { // We add all the materials on the WorkOrder to the first
						// Timetracking
			ArrayList<Material> materials = workOrder.getMaterials();
			for (int i = 0; i < materials.size(); i++) {
				Material material = materials.get(i);
				
				String code = material.getCode();
				
				if (ObjectDAO.getMaterial(token.getSoftwareToken(), code) == null) {
					throw new NoSuchElementException("Material on workorder " + workOrder.getWorkorderNr()
							+ " does not exist in WorkOrderApp\n");
				}
				
				String description_ = material.getDescription();
				double price = material.getPrice();
				String quantity = material.getQuantity();
				
				timetracking.add("description_" + i, description_);
				timetracking.add("price_" + i, Double.toString(price));
				timetracking.add("amount_" + i, quantity);
				timetracking.add("vat_" + i, "21");
				timetracking.add("product_id_" + i, code);
			}
		}
		return timetracking;
	}
	
	// =======================================================================//
	// Methods //
	// =======================================================================//
	
	/**
	 * A <tt>list</tt> of all the <tt>methods</tt> in {@link ObjectDAO}.
	 */
	private static List<Method> DB_METHODS;
	static {
		DB_METHODS = Arrays.asList(ObjectDAO.class.getMethods());
	}
	
	/**
	 * Get a <tt>method</tt> from {@link #DB_METHODS} by name.
	 *
	 * @param name
	 *            The name of the <tt>method</tt>.
	 *
	 * @return The <tt>method</tt>, or <code>null</code> if the method could not
	 *         be found.
	 */
	private Method forName(String name) {
		for (Method method : DB_METHODS) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		return null;
	}
	
}