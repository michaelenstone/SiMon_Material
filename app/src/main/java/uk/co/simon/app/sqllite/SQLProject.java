package uk.co.simon.app.sqllite;

import android.graphics.Color;

public class SQLProject {
	private long id;
	private String Project;
	private String ProjectNumber;
	private long cloudID = 0;
	public int projectColor;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
        int colorNumber = (int) id % 5;
        switch (colorNumber) {
            case 0:
                projectColor = Color.parseColor("#2196F3");
                break;
            case 1:
                projectColor = Color.parseColor("#ffcc00");
                break;
            case 2:
                projectColor = Color.parseColor("#F44336");
                break;
            case 3:
                projectColor = Color.parseColor("#009688");
                break;
            case 4:
                projectColor = Color.parseColor("#4CAF50");
                break;
        }
	}

	public String getProject() {
		return Project;
	}

	public String getProjectNumber() {
		return ProjectNumber;
	}

	public void setProject(String Project) {
		this.Project = Project;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return Project;
	}

	public void setProjectNumber(String ProjectNumber) {
		this.ProjectNumber = ProjectNumber;
	}
	
	public long getCloudID() {
		return cloudID;
	}
	
	public void setCloudID(long cloudID) {
		this.cloudID = cloudID;
	}

	public boolean equals(SQLProject project) {
		if (project.getProject() == this.Project && 
				project.getProjectNumber() == this.ProjectNumber &&
				project.getCloudID() == this.cloudID) {
			return true;
		} else {
			return false;
		}
	}
}
