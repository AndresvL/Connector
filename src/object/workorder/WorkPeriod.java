package object.workorder;

public class WorkPeriod {
	// UREN
	private String employeeNr, hourType, description, ratecode, projectNr, id, workDate, beginTime, endTime;
	private int duration;
	
	public WorkPeriod(String employeeNr, String hourType, String periodWorkDate, String projectNr, String description,
			String duration, String id, String bT, String eT) {
		this.employeeNr = employeeNr;
		this.hourType = hourType;
		this.setWorkDate(periodWorkDate);
		this.projectNr = projectNr;
		this.description = description;
		this.setDuration(duration);
		this.id = id;
		this.beginTime = bT;
		this.endTime = eT;
	}
	
	public String getProjectNr() {
		return projectNr;
	}
	
	public void setProjectNr(String projectNr) {
		this.projectNr = projectNr;
	}
	
	public String getDescription() {
		if (description == null) {
			return "leeg";
		} else {
			return description;
		}
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getEmployeeNr() {
		if (employeeNr == null) {
			return "leeg";
		} else {
			return employeeNr;
		}
	}
	
	public void setEmployeeNr(String employeeNr) {
		this.employeeNr = employeeNr;
	}
	
	public String getHourType() {
		if (hourType == null) {
			return "leeg";
		} else {
			return hourType;
		}
	}
	
	public int getDuration() {
		if (duration == 0) {
			return 0;
		} else {
			return duration;
		}
	}
	
	// Duration in minutes
	public void setDuration(String duration) {
		String[] time = duration.split(":");
		if (time.length > 1) {
			int hours = Integer.parseInt(time[0]);
			hours = hours * 60;
			int mins = Integer.parseInt(time[1]);
			int total = mins + hours;
			this.duration = total;
		} else {
			this.duration = Integer.parseInt(duration);
		}
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setHourType(String hourType) {
		this.hourType = hourType;
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
	
	public String getRatecode() {
		if (ratecode == null) {
			return "leeg";
		} else {
			return ratecode;
		}
	}
	
	public void setRatecode(String ratecode) {
		this.ratecode = ratecode;
	}
	
	public String getBeginTime() {
		return beginTime;
	}
	
	public void setBeginTime(String beginTime) {
		this.beginTime = beginTime;
	}
	
	public String getEndTime() {
		return endTime;
	}
	
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	
}
