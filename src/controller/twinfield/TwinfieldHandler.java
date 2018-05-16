package controller.twinfield;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import object.Settings;
import object.twinfield.Search;
import object.workorder.Address;
import object.workorder.Employee;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Project;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class TwinfieldHandler {
	private ArrayList<String> responseArray = null;
	private String invoiceType;
	private Search searchObject = null;
	private String[][] options = null;
	private String[] messageArray = null;
	private Boolean checkUpdate = false;
	private int factuurSuccess, factuurError, factuurAmount;
	private String hourString;
	private String factuurString = null;
	private String errorFactuurDetails = "";
	private String errorUrenDetails = "";
	private int urenError = 0;
	private String oldProjectNr = null;
	
	/**
	 * Returns a the current date minus 2 hours This method is used to set the
	 * sync date 2 hours before the system date
	 * 
	 * @param string
	 *            current date
	 * @return a new date minus 2 hours
	 */
	public String getDateMinHour(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			// Create Calender to edit time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR_OF_DAY, -2);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String s = formatter.format(date);
		return s;
	}
	
	/**
	 * Returns an array with error message and error details This method gets
	 * all the employees from Twinfield
	 * 
	 * @param office
	 *            the office in Twinfield from where the data is fetched
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param date
	 *            the current sync date
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 */
	public String[] getEmployees(String office, String session, String cluster, String token, String softwareName,
			String date) throws ServletException, IOException, SQLException {
		String errorMessage = "";
		// Create search object
		// Parameters: type, pattern, field, firstRow, maxRows, options
		options = new String[][] { { "ArrayOfString", "string", "office", office } };
		searchObject = new Search("USR", "*", 0, 1, 100, options);
		responseArray = SoapHandler.createSOAPFinder(session, cluster, searchObject);
		ArrayList<Employee> emp = new ArrayList<Employee>();
		// Split data from ArrayList
		
		for (int i = 0; i < responseArray.size(); i++) {
			String[] parts = responseArray.get(i).split(",");
			// firstName and Lastname are identical
			Employee e = new Employee(parts[1], parts[1], parts[0]);
			emp.add(e);
		}
		if (!emp.isEmpty()) {
			ObjectDAO.saveEmployees(emp, token);
			// Post data to WorkorderApp
			int successAmount = (int) WorkOrderHandler.addData(token, emp, "employees", softwareName);
			if (successAmount > 0) {
				errorMessage += successAmount + " medewerkers geïmporteerd<br />";
				checkUpdate = false;
			} else {
				errorMessage += "Er ging iets mis met de medewerkers<br />";
			}
		}
		messageArray = new String[] { errorMessage, checkUpdate + "" };
		return messageArray;
	}
	
	/**
	 * Returns an array with error message and error details This method gets
	 * all the projects from Twinfield
	 * 
	 * @param office
	 *            the office in Twinfield from where the data is fetched
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param date
	 *            the current sync date
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 * @throws SQLException
	 */
	public String[] getProjects(String office, String session, String cluster, String token, String softwareName,
			String date) throws ServletException, IOException, SQLException {
		String errorMessage = "";
		// Create search object
		// Parameters: type, pattern, field, firstRow, maxRows, options
		if (date != null) {
			options = new String[][] { { "ArrayOfString", "string", "dimtype", "PRJ" },
					{ "ArrayOfString", "string", "office", office },
					{ "ArrayOfString", "string", "modifiedsince", getDateMinHour(date) } };
		} else {
			options = new String[][] { { "ArrayOfString", "string", "dimtype", "PRJ" },
					{ "ArrayOfString", "string", "office", office } };
		}
		searchObject = new Search("DIM", "*", 0, 1, 100, options);
		responseArray = SoapHandler.createSOAPFinder(session, cluster, searchObject);
		ArrayList<Project> projects = new ArrayList<Project>();
		for (int i = 0; i < responseArray.size(); i++) {
			String[] parts = responseArray.get(i).split(",");
			// XMLString
			String string = "<read><type>dimensions</type><office>" + office + "</office><dimtype>PRJ</dimtype><code>"
					+ parts[0] + "</code></read>";
			Object obj = SoapHandler.createSOAPXML(session, cluster, string, "project");
			if (obj != null) {
				Project p = (Project) obj;
				projects.add(p);
				
			}
		}
		if (!projects.isEmpty()) {
			ObjectDAO.saveProjects(projects, token);
			int successAmount = (int) WorkOrderHandler.addData(token, projects, "projects", softwareName);
			if (successAmount > 0) {
				errorMessage += successAmount + " projecten geïmporteerd<br />";
				checkUpdate = true;
			} else {
				errorMessage += "Er ging iets mis met de projecten<br />";
			}
		} else {
			errorMessage += "Geen projecten voor import<br />";
		}
		messageArray = new String[] { errorMessage, checkUpdate + "" };
		return messageArray;
	}
	
	/**
	 * Returns an array with error message and error details This method gets
	 * all the materials from Twinfield
	 * 
	 * @param office
	 *            the office in Twinfield from where the data is fetched
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param date
	 *            the current sync date
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 * @throws SQLException
	 */
	public String[] getMaterials(String office, String session, String cluster, String token, String softwareName,
			String date) throws ServletException, IOException, SQLException {
		String errorMessage = "";
		// Create search object
		// Parameters: type, pattern, field, firstRow, maxRows, options
		options = new String[][] { { "ArrayOfString", "string", "office", office } };
		searchObject = new Search("ART", "*", 0, 1, 100, options);
		responseArray = SoapHandler.createSOAPFinder(session, cluster, searchObject);
		ArrayList<Material> materials = new ArrayList<Material>();
		for (int i = 0; i < responseArray.size(); i++) {
			String[] parts = responseArray.get(i).split(",");
			String string = "<read><type>article</type><office>" + office + "</office><code>" + parts[0]
					+ "</code></read>";
			Object obj = SoapHandler.createSOAPXML(session, cluster, string, "material");
			if (obj != null) {
				@SuppressWarnings("unchecked")
				ArrayList<Material> subMaterial = (ArrayList<Material>) obj;
				for (Material sub : subMaterial) {
					materials.add(sub);
				}
			}
		}
		if (!materials.isEmpty()) {
			ObjectDAO.saveMaterials(materials, token);
			int successAmount = (int) WorkOrderHandler.addData(token, materials, "materials", softwareName);
			if (successAmount > 0) {
				errorMessage += successAmount + " materialen geïmporteerd<br />";
				checkUpdate = false;
			} else {
				errorMessage += "Er ging iets mis met de materialen<br />";
			}
		} else {
			errorMessage += "Geen materialen voor import<br />";
		}
		messageArray = new String[] { errorMessage, checkUpdate + "" };
		return messageArray;
	}
	
	/**
	 * Returns an array with error message and error details This method gets
	 * all the relations from Twinfield
	 * 
	 * @param office
	 *            the office in Twinfield from where the data is fetched
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param date
	 *            the current sync date
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 * @throws SQLException
	 */
	public String[] getRelations(String office, String session, String cluster, String token, String softwareName,
			String date) throws ServletException, IOException, SQLException {
		String errorMessage = "";
		String dimType = "DEB";
		// Create search object
		// Parameters: type, pattern, field, firstRow, maxRows, options
		if (date != null) {
			options = new String[][] { { "ArrayOfString", "string", "dimtype", dimType },
					{ "ArrayOfString", "string", "office", office },
					{ "ArrayOfString", "string", "modifiedsince", getDateMinHour(date) } };
		} else {
			options = new String[][] { { "ArrayOfString", "string", "dimtype", dimType },
					{ "ArrayOfString", "string", "office", office } };
		}
		searchObject = new Search("DIM", "*", 0, 1, 100, options);
		responseArray = SoapHandler.createSOAPFinder(session, cluster, searchObject);
		ArrayList<Relation> relations = new ArrayList<Relation>();
		for (int i = 0; i < responseArray.size(); i++) {
			String[] parts = responseArray.get(i).split(",");
			String string = "<read><type>dimensions</type><office>" + office + "</office><dimtype>" + dimType
					+ "</dimtype><code>" + parts[0] + "</code></read>";
			Object obj = SoapHandler.createSOAPXML(session, cluster, string, "relation");
			if (obj != null) {
				Relation r = (Relation) obj;
				relations.add(r);
			}
		}
		if (!relations.isEmpty()) {
			ObjectDAO.saveRelations(relations, token);
			int successAmount = (int) WorkOrderHandler.addData(token, relations, "relations", softwareName);
			if (successAmount > 0) {
				errorMessage += successAmount + " relaties geïmporteerd<br />";
				checkUpdate = true;
			} else {
				errorMessage += "Er ging iets mis met de relaties<br />";
			}
		} else {
			errorMessage += "Geen relaties voor import<br />";
		}
		messageArray = new String[] { errorMessage, checkUpdate + "" };
		return messageArray;
	}
	
	public String[] getHourTypes(String office, String session, String cluster, String token, String softwareName,
			String date) throws ServletException, IOException, SQLException {
		String errorMessage = "";
		// Create search object
		// Parameters: type, pattern, field, firstRow, maxRows, options
		if (date != null) {
			options = new String[][] { { "ArrayOfString", "string", "office", office },
					{ "ArrayOfString", "string", "dimtype", "ACT" },
					{ "ArrayOfString", "string", "modifiedsince", getDateMinHour(date) } };
		} else {
			options = new String[][] { { "ArrayOfString", "string", "office", office },
					{ "ArrayOfString", "string", "dimtype", "ACT" } };
		}
		searchObject = new Search("DIM", "*", 0, 1, 100, options);
		responseArray = SoapHandler.createSOAPFinder(session, cluster, searchObject);
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		for (int i = 0; i < responseArray.size(); i++) {
			String[] parts = responseArray.get(i).split(",");
			String string = "<read><type>dimensions</type><office>" + office + "</office><dimtype>ACT</dimtype><code>"
					+ parts[0] + "</code></read>";
			Object obj = SoapHandler.createSOAPXML(session, cluster, string, "hourtype");
			if (obj != null) {
				HourType h = (HourType) obj;
				hourtypes.add(h);
			}
		}
		if (!hourtypes.isEmpty()) {
			ObjectDAO.saveHourTypes(hourtypes, token);
			int successAmount = (int) WorkOrderHandler.addData(token, hourtypes, "hourtypes", softwareName);
			if (successAmount > 0) {
				errorMessage = successAmount + " uursoorten geïmporteerd<br />";
				checkUpdate = true;
			} else {
				errorMessage += "Er ging iets mis met de uursoorten<br />";
			}
		} else {
			errorMessage += "Geen uursoorten voor import<br />";
		}
		messageArray = new String[] { errorMessage, checkUpdate + "" };
		return messageArray;
	}
	
	/**
	 * Returns an array with error message and error details This method gets
	 * all the workorders from WorkOrderApp
	 *
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param set
	 *            the saved settings of the user
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 */
	@SuppressWarnings("unchecked")
	public String[] setWorkOrders(String session, String cluster, String token, String softwareName, Settings set)
			throws SQLException {
		String user = set.getUser();
		String factuurType = set.getFactuurType();
		String office = set.getExportOffice();
		ArrayList<String> exportObjects = set.getExportObjects();
		String errorMessage = "";
		ArrayList<WorkOrder> allData = WorkOrderHandler.getData(token, "GetWorkorders", factuurType, false,
				softwareName);
		ArrayList<WorkOrder> tempUren = new ArrayList<WorkOrder>();
		if (allData.isEmpty() || allData == null) {
			System.out.println("allData is empty");
		}
		hourString = "<teqs>";
		for (WorkOrder w : allData) {
			if (w.getProjectNr().equals("")) {
				factuurAmount++;
				errorMessage = setFactuur(w, token, office, session, cluster, softwareName, exportObjects, null);
			} else {
				// set hourString
				errorMessage = setUurboeking(w, office, tempUren, user);
			}
		}
		hourString += "</teqs>";
		// -- LOG MESSAGES --
		// Factuur log
		if (factuurAmount > 0) {
			if (factuurError > 0) {
				if (factuurSuccess > 0) {
					errorMessage += factuurSuccess + " facturen aangemaakt<br />";
				}
				errorMessage += factuurError + " van de " + factuurAmount
						+ " werkbonnen voor facturatie hebben errors. klik voor meer details<br />";
				// errorFactuurDetails += "Zorg dat <b>alleen</b> de werkbonnen
				// die je wilt <b>factureren</b> op status <b>compleet</b>
				// staan";
			} else {
				// Factuur
				if (factuurSuccess > 0) {
					errorMessage += factuurSuccess + " facturen aangemaakt<br />";
				} else {
					errorMessage += "Geen werkbonnen voor facturatie gevonden<br />";
				}
			}
		}
		// Uren log
		if (!hourString.equals("<teqs></teqs>")) {
			ArrayList<String> results = (ArrayList<String>) SoapHandler.createSOAPXML(session, cluster, hourString,
					"workorder");
			int urenSuccess = 0;
			if (results != null) {
				for (String s : results) {
					if (s.equals("true")) {
						WorkOrder o = tempUren.get(urenSuccess);
						WorkOrderHandler.setWorkorderStatus(o.getId(), o.getWorkorderNr(), true, "GetWorkorder", token,
								softwareName);
						urenSuccess++;
					} else {
						urenError++;
						if (s != null && urenError < 5) {
							errorUrenDetails += s + "\n";
						}
					}
				}
			} else {
				errorUrenDetails += "Onbekende fout opgetreden" + "\n";
			}
			int urenAmount = urenSuccess + urenError;
			if (urenError > 0) {
				// errorUrenDetails += "Zorg dat <b>alleen</b> de werkbonnen
				// waarvan je de uren wilt boeken op status <b>compleet</b>
				// staan";
				if (urenSuccess > 0) {
					errorMessage += urenSuccess + " uurboekingen aangemaakt<br />";
				}
				errorMessage += urenError + " van de " + urenAmount
						+ " uurboekingen hebben errors. klik voor meer details<br />";
			} else {
				if (urenSuccess > 0) {
					errorMessage += urenSuccess + " uurboekingen aangemaakt<br />";
				} else {
					errorMessage += "Geen werkbonnen waarvan je de uren wilt boeken gevonden<br />";
				}
			}
		}
		// for (int k = 0; k < messageArray.length; k++) {
		// System.out.println("messageArray " + messageArray[k]);
		// }
		String details = errorFactuurDetails + errorUrenDetails;
		return new String[] { errorMessage, details };
	}
	
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}
	
	/**
	 * Returns a string with error messagesThis method gets all the employees
	 * from Twinfield
	 * 
	 * @param office
	 *            the office in Twinfield from where the data is fetched
	 * @param session
	 *            the current session of the user
	 * @param cluster
	 *            the current cluster of the user
	 * @param token
	 *            the current softwareToken of the user
	 * @param softwareName
	 *            the softwareName of the integration
	 * @param date
	 *            the current sync date
	 * @throws Exception
	 *             exception handling
	 * @return an array with error message and details
	 */
	private String setFactuur(WorkOrder w, String token, String office, String session, String cluster,
			String softwareName, ArrayList<String> exportObjects, String code) throws SQLException {
		String errorMessage = "";
		int factuur = 0;
		int post = 0;
		boolean hasMaterial = true;
		String debtorCode = null;
		post = ObjectDAO.getAddressID(token, "postal", w.getCustomerDebtorNr());
		factuur = ObjectDAO.getAddressID(token, "invoice", w.getCustomerDebtorNr());
		if (post == 0) {
			post = factuur;
		} else if (factuur == 0) {
			factuur = post;
		}
		if (code != null) {
			debtorCode = code;
			factuur = 1;
			post = 1;
		} else {
			debtorCode = w.getCustomerDebtorNr();
		}
		// + "<performancedate>" + w.getWorkDate() +
		// "</performancedate>"
		
		// format date
		String workDate = null;
		try {
			SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
			Date date = dt.parse(w.getCreationDate());
			SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
			workDate = dt1.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String status = w.getStatus();
		switch (status) {
		case "Afgehandeld":
			status = "final";
			break;
		case "Klaargezet":
			status = "concept";
			break;
		default:
			status = "concept";
			break;
		}
		String paymentMethod = w.getPaymentMethod();
		switch (paymentMethod) {
		case "pin betaling":
			paymentMethod = "bank";
			break;
		case "contant voldaan":
			paymentMethod = "cash";
			break;
		default:
			paymentMethod = "bank";
			break;
		}
		
		invoiceType = "FACTUUR";
		factuurString = "<salesinvoice>" + "<header>" + "<office>" + office + "</office>" + "<invoicetype>"
				+ invoiceType + "</invoicetype>" + "<invoicedate>" + workDate + "</invoicedate>" + "<customer>"
				+ debtorCode + "</customer>" + "<status>" + status + "</status>" + "<paymentmethod>" + paymentMethod
				+ "</paymentmethod>" + "<invoiceaddressnumber>" + factuur + "</invoiceaddressnumber>"
				+ "<deliveraddressnumber>" + post + "</deliveraddressnumber>" + "</header>" + "<lines>";
		int i = 0;
		for (Material m : w.getMaterials()) {
			String sub;
			if (m != null) {
				Material dbMaterial = ObjectDAO.getMaterial(token, m.getCode());
				if (dbMaterial == null) {
					hasMaterial = false;
				}
				i++;
				if (m.getSubCode() == null) {
					sub = "";
				} else {
					sub = m.getSubCode();
				}
				factuurString += "<line id=\"" + i + "\">" + "<article>" + m.getCode() + "</article>" + "<subarticle>"
						+ sub + "</subarticle>" + "<quantity>" + m.getQuantity() + "</quantity>" + "<units>"
						+ m.getUnit() + "</units>" + "</line>";
			} else {
				errorMessage += "Een materiaal op werkbon " + w.getWorkorderNr() + " bestaat niet in Twinfield<br />";
				factuurString = "";
			}
		}
		factuurString += "</lines></salesinvoice>";
		// If material exist
		if (!factuurString.equals("</lines></salesinvoice>")) {
			String resultsFactuur = (String) SoapHandler.createSOAPXML(session, cluster, factuurString,
					"workorderFactuur");
			if (resultsFactuur != null && resultsFactuur.equals("true")) {
				WorkOrderHandler.setWorkorderStatus(w.getId(), w.getWorkorderNr(), true, "GetWorkorder", token,
						softwareName);
				factuurSuccess++;
			} else {
				// Create new Relation
				if (w.getMaterials().size() > 0 && hasMaterial) {
					if (factuur == 0 && exportObjects != null && exportObjects.contains("relations")) {
						Relation r = w.getRelations().get(0);
						Address a = r.getAddressess().get(0);
						
						String relationString = "<dimension>" + "<office>" + office + "</office>" + "<type>DEB</type>"
								+ "<name>" + r.getCompanyName() + "</name>" + "<addresses>"
								+ "<address id=\"1\" type=\"invoice\" default=\"true\">" + "<name>" + r.getCompanyName()
								+ "</name>" + "<country>NL</country>" + "<city>" + a.getCity() + "</city>"
								+ "<postcode>" + a.getPostalCode() + "</postcode>" + "<telephone>" + a.getPhoneNumber()
								+ "</telephone>" + "<email>" + a.getEmail() + "</email>" + "<field1>" + a.getName()
								+ "</field1>" + "<field2>" + a.getStreet() + "</field2>" + "</address>" + "</addresses>"
								+ "</dimension>";
						String resultsExportRelation = (String) SoapHandler.createSOAPXML(session, cluster,
								relationString, "workorderRelation");
						System.out.println("RELATION CREATED");
						if (resultsExportRelation != null && isInteger(resultsExportRelation)) {
							setFactuur(w, token, office, session, cluster, softwareName, exportObjects,
									resultsExportRelation);
						}
					} else {
						// Twinfield soapResponse
						factuurError++;
						if (resultsFactuur != null && factuurError < 5) {
							if (code != null) {
								String relationString = "<dimension status='delete'>" + "<office>" + office
										+ "</office>" + "<type>DEB</type><code>" + code + "</code>" + "</dimension>";
								String resultsExportRelation = SoapHandler.createSOAPXML(session, cluster,
										relationString, "workorderRelationDelete") + "";
								if (isInteger(resultsExportRelation)) {
									System.out.println("RELATION VERWIJDERD");
								} else {
									System.out.println("RELATION NIET VERWIJDERD " + resultsExportRelation);
								}
							}
							errorFactuurDetails += resultsFactuur + "\n";
						}
					}
				} else {
					factuurError++;
					errorFactuurDetails += "Geen materialen gevonden op werkbon " + w.getWorkorderNr()
							+ ",\n Of materialen bestaan niet in Twinfield\n";
				}
			}
		}
		return errorMessage;
		
	}
	
	private String setUurboeking(WorkOrder w, String office, ArrayList<WorkOrder> tempUren, String user) {
		String code = "DIRECT";
		String errorMessage = "";
		for (WorkPeriod period : w.getWorkPeriods()) {
			if (!period.getHourType().equals("")) {
				if (user.equals("Geen")) {
					user = period.getEmployeeNr();
				}
				if (!user.equals("") && user != null) {
					// format date
					String workDate = null;
					try {
						SimpleDateFormat dt = new SimpleDateFormat("dd-MM-yyyy");
						Date date = dt.parse(period.getWorkDate());
						SimpleDateFormat dt1 = new SimpleDateFormat("yyyyMMdd");
						workDate = dt1.format(date);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					tempUren.add(w);
					hourString += "<teq>" + "<header>" + "<office>" + office + "</office>"
					// Check this later
							+ "<code>" + code + "</code>" + "<user>" + user + "</user>" + "<date>" + workDate
							+ "</date>" + "<prj1>" + period.getProjectNr() + "</prj1>" + "<prj2>" + period.getHourType()
							+ "</prj2>" + "</header>" + "<lines>" + "<line type= \"TIME\">" + "<duration>"
							+ period.getDuration() + "</duration>" + "<description>" + period.getDescription()
							+ "</description>" + "</line>" + "<line type=\"QUANTITY\">" + "</line>" + "</lines></teq>";
				} else {
					urenError++;
					if (urenError < 5) {
						errorMessage += "Een werkbon met projectnummer " + w.getProjectNr()
								+ " heeft een werkperiode zonder medewerker<br />";
					}
				}
			} else {
				urenError++;
				// Filter the results a little bit
				if (!w.getProjectNr().equals(oldProjectNr)) {
					if (urenError < 5) {
						errorMessage += "Een werkbon met projectnummer " + w.getProjectNr()
								+ " heeft een werkperiode zonder werktype<br />";
						oldProjectNr = w.getProjectNr();
					}
				}
			}
		}
		return errorMessage;
	}
}
