package fr.inria.main.spl;

public class Patch {
    private String location;
    private int lineNumber;
    private String correct_code;

    public Patch(String _location, int _lineNumber, String _correct_code){
        location = _location;
        lineNumber = _lineNumber;
        correct_code = _correct_code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCorrect_code() {
        return correct_code;
    }

    public void setCorrect_code(String correct_code) {
        this.correct_code = correct_code;
    }
}
