package object.workorder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Project {
	private String code, codeExtern, debtorNumber, status, name, dateStart, dateEnd, description, authoriser;
	private int progress, active;
	
	public Project() {
	}
	
	public Project(String code, String name) {
		this.code = code;
		this.name = name;
	}
	
	public Project(String code, String code_ext, String debtor_number, String status, String name, String date_start,
			String date_end, String description, int progress, int active, String authoriser) {
		this.code = code;
		this.codeExtern = code_ext;
		this.debtorNumber = debtor_number;
		this.status = status;
		this.name = name;
		this.setDateStart(date_start);
		this.setDateEnd(date_end);
		this.description = description;
		this.progress = progress;
		this.active = active;
		this.setAuthoriser(authoriser);
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getCodeExt() {
		return codeExtern;
	}
	
	public void setCodeExt(String code_ext) {
		this.codeExtern = code_ext;
	}
	
	public String getDebtorNumber() {
		if (debtorNumber == null || debtorNumber.equals("")) {
			debtorNumber = "empty";
		}
		return debtorNumber;
	}
	
	public void setDebtorNumber(String debtor_number) {
		this.debtorNumber = debtor_number;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDateStart() {
		if (dateStart == null) {
			return "2017-01-01";
		} else {
			return dateStart;
		}
	}
	
	public void setDateStart(String date_start) {
		if (date_start != null && !date_start.equals("") && date_start.split("-").length != 3) {
			try {
				SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
				Date date = dt.parse(date_start);
				SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
				this.dateStart = dt1.format(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getDateEnd() {
		// if(dateEnd == null){
		// return "2017-01-01";
		// }else{
		// return dateEnd;
		// }
		return dateEnd;
	}
	
	public void setDateEnd(String date_end) {
		if (date_end != null && !date_end.equals("") && date_end.split("-").length != 3) {
			try {
				SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
				Date date = dt.parse(date_end);
				SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
				this.dateEnd = dt1.format(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
	}
	
	public int getActive() {
		return active;
	}
	
	public void setActive(int active) {
		this.active = active;
	}
	
	public String getAuthoriser() {
		return authoriser;
	}
	
	public void setAuthoriser(String authoriser) {
		this.authoriser = authoriser;
	}
	
}
