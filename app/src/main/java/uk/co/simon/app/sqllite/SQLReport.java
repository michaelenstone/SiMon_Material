package uk.co.simon.app.sqllite;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SQLReport {
	private long id;
	private long projectId = 0;
	private Date reportDate;
	private boolean reportType = true;
	private String supervisor = " ";
	private String reportRef = " ";
	private String weather = " ";
	private String temp = " ";
	private long tempType = 0;
	private String PDF = " ";
	private boolean hasPDF = false;
	private long cloudID = 0;

    private static DateFormat inputFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
    private static DateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getTempType() {
		return tempType;
	}

	//true=Farenheit false=Celcius
	public void setTempType(long tempType) {
		this.tempType = tempType;
	}

	public String getTemp() {
		return temp;
	}

	public void setTemp(String temp) {
		this.temp = temp;
	}

	public String getWeather() {
		return weather;
	}

	public void setWeather(String weather) {
		this.weather = weather;
	}

	public String getReportRef() {
		return reportRef;
	}

	public void setReportRef(String reportRef) {
		this.reportRef = reportRef;
	}

	public String getSupervisor() {
		return supervisor;
	}

	public void setSupervisor(String supervisor) {
		this.supervisor = supervisor;
	}

	public boolean getReportType() {
		return reportType;
	}

	//true=SVR false=Progress
	public void setReportType(boolean reportType) {
		this.reportType = reportType;
	}

	public String getReportDate() {
		return outputFormat.format(reportDate);
	}

	public String getReportDateDB() {
		return inputFormat.format(reportDate);
	}

	public void setReportDate(String reportDate) {
        try {
            this.reportDate = outputFormat.parse(reportDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

	public void setReportDateDB(String reportDate) {
        try {
            this.reportDate = inputFormat.parse(reportDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
	}

	public long getProjectId() {
		return projectId;
	}

	public void setProjectId(long projectId) {
		this.projectId = projectId;
	}

	public String getPDF() {
		return PDF;
	}

	public void setPDF(String Path) {
		File pdf = new File(Path);
		if (pdf.exists()) {
			PDF = Path;
			hasPDF = true;
		}
	}

	public boolean hasPDF() {
		return this.hasPDF;
	}

	public long getCloudID() {
		return cloudID;
	}

	public void setCloudID(long cloudID) {
		this.cloudID = cloudID;
	}
}
