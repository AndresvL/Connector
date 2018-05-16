package object.workorder;

import java.util.ArrayList;

public class WorkOrder {
	// FACTUUR
	// Header
	// ProjectNr, performancedate, invoiceaddressnumber,
	// deliveraddressnumber, customercode, status, paymentmethod(cash, bank,
	// cheque, cashondelivery, da)
	private String id, workorderNr, projectNr, workDate, customerEmailInvoice, customerEmail, customerDebtorNr, status,
			paymentMethod, creationDate, workTime, workEndDate, workEndTime, externProjectNr, typeOfWork,
			workDescription, modified, pdfUrl, workStatus, employeeNr;
	// line
	private ArrayList<Material> materials;
	private ArrayList<WorkPeriod> workPeriods;
	private ArrayList<Relation> relations;
	
	// FACTUUR constructor
	public WorkOrder(String projectNr, String workDate, String customerEmailInvoice, String customerEmail,
			String customerDebtorNr, String status, String paymentMethod, ArrayList<Material> m, String creationDate,
			String id, String workorderNr, ArrayList<WorkPeriod> work, ArrayList<Relation> relation, String workTime,
			String workEndDate, String workEndTime, String externProjectNr, String typeOfWork, String workDescription,
			String modified, String pdfUrl, String workStatus, String employeeNr) {
		this.projectNr = projectNr; // priceQuote
		this.setWorkDate(workDate); // date
		this.customerEmailInvoice = customerEmailInvoice; // EmailAddress
		this.customerEmail = customerEmail;// EmailAddress
		this.customerDebtorNr = customerDebtorNr; // DebtorCode
		this.status = status; // Status
		this.paymentMethod = paymentMethod;
		this.materials = m; // ProductCode...
		this.setCreationDate(creationDate);
		this.id = id;// Identifier
		this.workorderNr = workorderNr;// PriceQuoteCode
		this.workPeriods = work; // ProductCode...
		this.relations = relation;
		this.workTime = workTime;
		this.workEndDate = workEndDate;
		this.workEndTime = workEndTime;
		this.setExternProjectNr(externProjectNr);
		this.typeOfWork = typeOfWork;
		this.workDescription = workDescription;
		this.setModified(modified);
		this.setPdfUrl(pdfUrl);
		this.setWorkStatus(workStatus);
		this.setEmployeeNr(employeeNr);
	}
	
	public String getProjectNr() {
		return projectNr;
	}
	
	public void setProjectNr(String projectNr) {
		this.projectNr = projectNr;
	}
	
	public String getWorkDate() {
		if (workDate == null) {
			return "leeg";
		} else {
			return workDate;
		}
	}
	
	public void setWorkDate(String workDate) {
		if (workDate != null) {
			this.workDate = workDate;
		}
	}
	
	public String getCustomerEmailInvoice() {
		return customerEmailInvoice;
	}
	
	public void setCustomerEmailInvoice(String customerEmailInvoice) {
		this.customerEmailInvoice = customerEmailInvoice;
	}
	
	public String getCustomerEmail() {
		return customerEmail;
	}
	
	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}
	
	public String getCustomerDebtorNr() {
		return customerDebtorNr;
	}
	
	public void setCustomerDebtorNr(String customerDebtorNr) {
		this.customerDebtorNr = customerDebtorNr;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getPaymentMethod() {
		return paymentMethod;
	}
	
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	public ArrayList<Material> getMaterials() {
		return materials;
	}
	
	public void setMaterials(ArrayList<Material> materials) {
		this.materials = materials;
	}
	
	public String getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	
	public String getWorkorderNr() {
		return workorderNr;
	}
	
	public void setWorkorderNr(String workorderNr) {
		this.workorderNr = workorderNr;
	}
	
	public ArrayList<WorkPeriod> getWorkPeriods() {
		return workPeriods;
	}
	
	public void setWorkPeriods(ArrayList<WorkPeriod> work) {
		this.workPeriods = work;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public ArrayList<Relation> getRelations() {
		return relations;
	}
	
	public void setRelations(ArrayList<Relation> relations) {
		this.relations = relations;
	}
	
	public String getWorkTime() {
		return workTime;
	}
	
	public void setWorkTime(String workTime) {
		this.workTime = workTime;
	}
	
	public String getWorkEndDate() {
		return workEndDate;
	}
	
	public void setWorkEndDate(String workEndDate) {
		this.workEndDate = workEndDate;
	}
	
	public String getWorkEndTime() {
		return workEndTime;
	}
	
	public void setWorkEndTime(String workEndTime) {
		this.workEndTime = workEndTime;
	}
	
	public String getExternProjectNr() {
		return externProjectNr;
	}
	
	public void setExternProjectNr(String externProjectNr) {
		this.externProjectNr = externProjectNr;
	}
	
	public String getTypeOfWork() {
		return typeOfWork;
	}
	
	public void setTypeOfWork(String typeOfWork) {
		this.typeOfWork = typeOfWork;
	}
	
	public String getWorkDescription() {
		return workDescription;
	}
	
	public void setWorkDescription(String workDescription) {
		this.workDescription = workDescription;
	}
	
	public String getModified() {
		return modified;
	}
	
	public void setModified(String modified) {
		this.modified = modified;
	}
	
	public String getPdfUrl() {
		return pdfUrl;
	}
	
	public void setPdfUrl(String pdfUrl) {
		this.pdfUrl = pdfUrl;
	}
	
	public String getWorkStatus() {
		return workStatus;
	}
	
	public void setWorkStatus(String workStatus) {
		this.workStatus = workStatus;
	}
	
	public String getEmployeeNr() {
		return employeeNr;
	}
	
	public void setEmployeeNr(String employeeNr) {
		this.employeeNr = employeeNr;
	}
	
}
