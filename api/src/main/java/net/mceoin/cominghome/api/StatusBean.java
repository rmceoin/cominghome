package net.mceoin.cominghome.api;

/** The object model for the data we are sending through endpoints */
public class StatusBean {

    private boolean success;
    private String message;
    private boolean nestSuccess;
    private boolean nestUpdated;
    private boolean othersAtHome;

    public boolean isOthersAtHome() {
        return othersAtHome;
    }

    public void setOthersAtHome(boolean othersAtHome) {
        this.othersAtHome = othersAtHome;
    }

    public boolean isNestUpdated() {
        return nestUpdated;
    }

    public void setNestUpdated(boolean nestUpdated) {
        this.nestUpdated = nestUpdated;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isNestSuccess() {
        return nestSuccess;
    }

    public void setNestSuccess(boolean nestSuccess) {
        this.nestSuccess = nestSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}